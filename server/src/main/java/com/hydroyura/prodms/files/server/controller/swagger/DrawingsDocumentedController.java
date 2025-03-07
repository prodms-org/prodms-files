package com.hydroyura.prodms.files.server.controller.swagger;


import com.hydroyura.prodms.common.model.api.ApiRes;
import com.hydroyura.prodms.files.server.api.drawings.params.GetLatestParams;
import com.hydroyura.prodms.files.server.api.enums.DrawingType;
import com.hydroyura.prodms.files.server.api.req.AddFileReq;
import com.hydroyura.prodms.files.server.api.res.GetUrlsLatestRes;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public interface DrawingsDocumentedController {


    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            content = {@Content(schema = @Schema(implementation = GetLatestSuccess.class))},
            description = "Success"
        )
    })
    ResponseEntity<ApiRes<?>> getFiles(String number, GetLatestParams params);
    class GetLatestSuccess extends ApiRes<GetUrlsLatestRes> {}


    @RequestMapping(method = RequestMethod.POST, value = "api/v1/drawings/{number}")
    ResponseEntity<ApiRes<?>> addFile(String number, AddFileReq req);


}
