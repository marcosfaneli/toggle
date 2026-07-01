package com.toggle.server.toggle.web;

import com.toggle.server.toggle.application.ListMaintainersUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/maintainers")
public class MaintainerController {

    private final ListMaintainersUseCase listMaintainersUseCase;

    public MaintainerController(ListMaintainersUseCase listMaintainersUseCase) {
        this.listMaintainersUseCase = listMaintainersUseCase;
    }

    @GetMapping
    public List<String> list() {
        return listMaintainersUseCase.execute();
    }
}
