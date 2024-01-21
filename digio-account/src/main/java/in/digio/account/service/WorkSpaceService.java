package in.digio.account.service;


import in.digio.account.constants.AccountErrorCodes;
import in.digio.account.dto.WorkSpaceRequest;
import in.digio.account.dto.WorkSpaceResponse;
import in.digio.account.exception.AccountException;
import in.digio.account.model.WorkSpace;
import in.digio.account.repository.WorkSpaceRepository;
import in.digio.acls.service.GrantPermissionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkSpaceService {

    private final WorkSpaceRepository workSpaceRepository;
    private final GrantPermissionManager grantPermissionManager;

    @Transactional
    public WorkSpaceResponse createWorkSpace(WorkSpaceRequest workSpaceRequest) {
        long workSpaceCount = workSpaceRepository.count();
        if (workSpaceCount >= 1) {
            throw new AccountException(AccountErrorCodes.ACCOUNT_LIMIT_EXCEEDED);
        }
        WorkSpace workSpace = workSpaceRepository.save(WorkSpace.builder().name(workSpaceRequest.getName()).build());
        grantPermissionManager.setPermissionToEntity(workSpace);
        return WorkSpaceResponse.builder().id(workSpace.getId()).name(workSpace.getName()).build();
    }


    @Transactional(readOnly = true)
    public List<WorkSpaceResponse> getWorkSpaces() {
        List<WorkSpace> workSpaces = workSpaceRepository.findAll();
        return workSpaces.stream().map(workSpace -> WorkSpaceResponse.builder().id(workSpace.getId()).name(workSpace.getName()).build()).toList();
    }


}
