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

    public Mono<Void> getStockOAuthAndSave(Long authenticatedUserId, StockOAuthSaveRequest dto) {
        return brokerClient.getOAuthToken(DTOMapper.toStockOAuthRequest(authenticatedUserId, dto))
            .flatMap(oAuthToken ->
                userRepository.findById(authenticatedUserId)
                    .flatMap(user -> {
                        user.setMarketAccessToken(oAuthToken);
                        return userRepository.save(user);
                    })
                    .doOnSuccess(user -> log.debug("Broker token saved, userId={}", user.getId()))
                    .then()
            );
    }
}
