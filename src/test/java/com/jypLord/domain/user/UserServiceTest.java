package com.jypLord.domain.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.handler.BrokerClient;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BrokerClient brokerClient;

    @InjectMocks
    private UserService userService;

    @Test
    void getStockOAuthAndSave_success() {
        LsStockOAuthSaveRequest request = new LsStockOAuthSaveRequest(BrokerageFirm.LS, "app-key", "app-secret");
        User user = new User(1L, "test@test.com", "pw", "name", LocalDate.of(2000, 1, 1), null, null);

        given(brokerClient.getOAuthToken(any())).willReturn(Mono.just("oauth-token"));
        given(userRepository.findById(1L)).willReturn(Mono.just(user));
        given(userRepository.save(any(User.class))).willReturn(Mono.just(user));

        StepVerifier.create(userService.getStockOAuthAndSave(1L, request))
            .verifyComplete();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void getStockOAuthAndSave_userNotFound_completesWithoutSave() {
        LsStockOAuthSaveRequest request = new LsStockOAuthSaveRequest(BrokerageFirm.LS, "app-key", "app-secret");

        given(brokerClient.getOAuthToken(any())).willReturn(Mono.just("oauth-token"));
        given(userRepository.findById(99L)).willReturn(Mono.empty());

        StepVerifier.create(userService.getStockOAuthAndSave(99L, request))
            .verifyComplete();

        verify(userRepository, never()).save(any());
    }
}
