package com.jypLord.domain.user;

import com.jypLord.api.handler.BrokerClient;
import com.jypLord.domain.user.dto.request.StockOAuthSaveRequest;
import com.jypLord.util.DTOMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BrokerClient brokerClient;

    /*
    증권사에서 OAuthToken 받아서 증권사 접근권한 취득, 그리고 DB에 저장.
     */
    public Mono<Void> getStockOAuthAndSave(StockOAuthSaveRequest dto){

        return brokerClient.getOAuthToken(DTOMapper.toStockOAuthRequest(dto))
            .flatMap(oAuthToken ->
                userRepository.findById(dto.getUserId())
                    .flatMap(user-> {
                        user.setMarketAccessToken(oAuthToken);

                        return userRepository.save(user);
                    })
                    .doOnSuccess(user-> log.debug("증권사 토큰 저장 성공, userId={}", user.getId()))
                    .then()
            );
    }


}
