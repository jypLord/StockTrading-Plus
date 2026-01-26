package com.jypLord.domain.user;


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
    public Mono<ResponseEntity<Void>> getOAuthTokenFromBroker(@RequestBody StockOAuthSaveRequest dto) {

        return userService.getStockOAuthAndSave(dto)
            .thenReturn(ResponseEntity.ok().build());
    }
}
