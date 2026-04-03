package com.jypLord.usertest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.handler.BrokerClient;
import com.jypLord.domain.user.User;
import com.jypLord.domain.user.UserRepository;
import com.jypLord.domain.user.UserService;
import com.jypLord.domain.user.dto.request.LsStockOAuthSaveRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BrokerClient brokerClient;

    @InjectMocks
    private UserService userService;

    @Test
    void get_accessToken_test() {
        LsStockOAuthSaveRequest request = new LsStockOAuthSaveRequest(BrokerageFirm.LS, "appKey", "appSecret");
        User user = new User(1L, "test@test.com", "pw", "name", LocalDate.of(2000, 1, 1), null, null);

        given(brokerClient.getOAuthToken(any())).willReturn(Mono.just("access-token"));
        given(userRepository.findById(1L)).willReturn(Mono.just(user));
        given(userRepository.save(any(User.class))).willReturn(Mono.just(user));

        StepVerifier.create(userService.getStockOAuthAndSave(1L, request))
            .verifyComplete();
    }
}
