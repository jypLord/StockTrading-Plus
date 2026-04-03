package com.jypLord.domain.user;

import com.jypLord.auth.jwt.AuthenticatedUser;
import com.jypLord.domain.user.dto.request.StockOAuthSaveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/stockOAuth")
    public Mono<ResponseEntity<Void>> getOAuthTokenFromBroker(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestBody StockOAuthSaveRequest dto
    ) {
        return userService.getStockOAuthAndSave(authenticatedUser.id(), dto)
            .thenReturn(ResponseEntity.ok().build());
    }
}
