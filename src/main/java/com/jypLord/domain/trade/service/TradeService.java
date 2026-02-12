package com.jypLord.domain.trade.service;
import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.dto.request.buy.request.BuyRequest;
import com.jypLord.api.dto.request.sell.SellRequest;
import com.jypLord.redis.pub.RedisAssetPricePublisher;
import com.jypLord.redis.streams.publisher.RedisStockEventPublisher;
import com.jypLord.redis.sub.RedisStockPriceSubscriber;
import com.jypLord.domain.trade.dto.request.RegisterTradeInfoRequest;
import com.jypLord.api.handler.BrokerClient;
import com.jypLord.domain.trade.TradeStatus;
import com.jypLord.domain.trade.entity.Trade;
import com.jypLord.domain.trade.repository.TradeRepository;
import com.jypLord.domain.user.User;
import com.jypLord.domain.user.UserRepository;
import com.jypLord.exception.broker.BrokerException;
import com.jypLord.exception.trade.NoValidTradeException;
import com.jypLord.redis.sub.StockPrice;
import com.jypLord.util.DTOMapper;
import java.rmi.AlreadyBoundException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final BrokerClient brokerClient;

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;

    private final RedisAssetPricePublisher redisPricePublisher;
    private final RedisStockPriceSubscriber redisPriceSubscriber;

    private final RedisStockEventPublisher redisStockEventPublisher;


    private final Map<Long, Set<String>> userSubscribeStockMap = new ConcurrentHashMap<>();


    /*
    관리 대상 종목 정보 저장 , 손절가 설정 & 수정까지
     */
    public Mono<Void> registerTradeInfo(RegisterTradeInfoRequest dto){

        return tradeRepository.findByUserIdAndStockCodeAndStatus(dto.userId(), dto.stockCode(), TradeStatus.ACTIVE)
            .flatMap(trade -> {

                if(trade.getUserSetPrice() == dto.price()) return Mono.error(new AlreadyBoundException("이미 추가된 종목임"));

                return Mono.error(new AlreadyBoundException("이미 추가된 종목임"));
            })
            .switchIfEmpty(tradeRepository.save( new Trade(dto.userId(), dto.stockCode(),dto.firm() ,dto.price(), dto.quantity(), TradeStatus.ACTIVE)))
            .then();
    }

    public Flux<Void> manageAsset(Long userId, BrokerageFirm firm){

        Mono<User> userCached = userRepository.findById(userId).cache();

        return tradeRepository.findValidTradeByUserId(userId, firm)
            .switchIfEmpty(Mono.error(new NoValidTradeException("등록된 데이터가 없음")))
            .take(10)

            // 유저마다 호출한 종목들 Map 으로 관리
            .doOnNext(trade ->
                userSubscribeStockMap
                .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(trade.getStockCode())
            )

            // publish 안되고 있는 애들만 골라내기
            .filterWhen( trade ->
                redisPricePublisher.acquireLockIfAbsent(trade.getStockCode(), userId)
            )

            .flatMap( trade ->

                userCached
                    .flatMapMany(user->

                        brokerClient.receivePrice(DTOMapper.toPriceRequest(userId, firm, user.getMarketAccessToken(), trade.getStockCode()))
                            .flatMap(asset ->

                                redisPricePublisher.publishIfLockOwner(asset)

                                    .onErrorResume(e ->
                                            e instanceof BrokerException || e instanceof TimeoutException,
                                        e-> redisPricePublisher.removeLockIfOwner(asset.sourceUserId(), asset.stockCode())
                                            .then(redisStockEventPublisher.publishBrokerSessionTerminatedEvent(asset.stockCode()))
                                    )
                            ).then(losscutMonitoring( userId, trade.getId(), user.getMarketAccessToken(), firm, trade.getStockCode(),
                                trade.getUserSetPrice(), trade.getQuantity())))
                    );
    }


    /*
    주가 감시 손절가 도달시 손절
     */
    public Mono<Void> losscutMonitoring(
        Long userId, Long tradeId, String marketAccessToken, BrokerageFirm firm, String stockCode,int userSetPrice, int quantity
    ) {

       return redisPriceSubscriber.subscribe(stockCode)
            // 사용자가 설정한 가격보다 현재가가 낮아지면
           .filter(info -> info.price() <= userSetPrice)
            // 한 건의 데이터만 받아서
           .take(1)
           .filterWhen(stock-> updateTradeStatusForIdempotency(tradeId, TradeStatus.ACTIVE, TradeStatus.EXECUTED_LOSSCUT))

           // 팔고, 재매수 감시를 위한 이벤트 발행
           .flatMap(info -> brokerClient.sell(new SellRequest(firm, stockCode, userSetPrice, quantity, marketAccessToken))
               .and(redisStockEventPublisher.publishLosscutEvent(userId, tradeId, stockCode, firm, userSetPrice, quantity))
           )
           .then();
    }
    /*
    * 증권사 API를 통해 실제 매수/매도 전, 신호의 멱등성 체크를 위하여 먼저 호출.
    * 멱등키로 사용된 tradeId 와 또 강한 멱등성 보장을 위해 Trade의 Status 를 검증
    * */
    public Mono<Boolean> updateTradeStatusForIdempotency(Long tradeId, TradeStatus expectedStatus, TradeStatus newStatus) {
        return tradeRepository.updateTradeStatus(tradeId, expectedStatus, newStatus);
    }

   /*
   * 손절한 종목을 손절가에 재매수
   */
    public Mono<Void> reBuyAfterLossCut(Long IdempotencyKey, Long userId, Flux<StockPrice> currentPrice, BrokerageFirm firm , int losscutPrice, int quantity) {

        return userRepository.findById(userId)
            .flatMapMany(user ->
                currentPrice
                    .filter(stock -> stock.price() > losscutPrice)
                    .next()
                    .filterWhen(event -> updateTradeStatusForIdempotency(IdempotencyKey, TradeStatus.EXECUTED_LOSSCUT, TradeStatus.EXECUTED_BUY ))
                    .flatMap(risingStock -> brokerClient.buy(new BuyRequest(firm, risingStock.stockCode(), losscutPrice, quantity, user.getMarketAccessToken())))
            ).then();
    }
}



