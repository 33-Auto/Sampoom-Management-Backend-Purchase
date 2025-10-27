package com.sampoom.purchase.api.health;

import com.sampoom.purchase.common.response.ApiResponse;
import com.sampoom.purchase.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "HealthCheck", description = "HealthCheck 관련 API 입니다.")
@RestController
@RequestMapping()
public class HealthCheckController {


    @GetMapping("/health")
    /* Swagger 작성예시     */
    @Operation(
            summary = "HealthCheck API",
            description = "서버 상태 체크 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "서버 상태 OK"),
    })

    public ResponseEntity<ApiResponse<Void>> healthCheck() {
        return ApiResponse.success_only(SuccessStatus.OK);
    }



}
