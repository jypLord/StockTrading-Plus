package com.jypLord.usertest;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.domain.user.UserRepository;
import com.jypLord.domain.user.UserService;
import com.jypLord.domain.user.dto.request.LsStockOAuthSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Slf4j
@SpringBootTest(properties = {
    "jwt.secret.key=test-secret-test-secret-test-secret-test-secret",
    "spring.r2dbc.url=r2dbc:mysql://localhost:3306/autoInvest",
    "spring.r2dbc.username=root",
    "spring.r2dbc.password=hanafos1!!"
})
public class UserTest {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Test
    void get_accessToken_test(){

        Mono<Void> mono =
            userService.getStockOAuthAndSave(new LsStockOAuthSaveRequest(BrokerageFirm.LS, 1L,
                "PSrw9BZqh1c6vxAksNo1S7s2UK7mHbBtOFwN",
                "YIyJo2iXD0feZiM6sLZLpOGwdJxIHC0s"
                ));

        StepVerifier.create(
                mono.then(userRepository.findById(1L))
            )
            .assertNext(user -> {
                assertThat(user.getMarketAccessToken()).isNotBlank();
            })
            .verifyComplete();
    }
}
