package in.digio.account.controller;


import in.digio.account.dto.WorkSpaceRequest;
import in.digio.account.dto.WorkSpaceResponse;
import in.digio.account.service.WorkSpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/account/workspace", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class WorkSpaceController {

    private final WorkSpaceService workSpaceService;

    @PostMapping(value = "")
    public WorkSpaceResponse createWorkSpace(@RequestBody WorkSpaceRequest workSpaceRequest) {
        return workSpaceService.createWorkSpace(workSpaceRequest);
    }

    @GetMapping(value = "")
    public List<WorkSpaceResponse> getWorkSpace() {
        return workSpaceService.getWorkSpaces();
    }
}
