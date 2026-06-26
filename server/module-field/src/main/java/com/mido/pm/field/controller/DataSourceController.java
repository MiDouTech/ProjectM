package com.mido.pm.field.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.field.dto.DataSourceSaveDTO;
import com.mido.pm.field.dto.DataSourceVO;
import com.mido.pm.field.service.DataSourceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 数据源（选项集库）CRUD（租户自配）。 */
@RestController
@RequestMapping("/api/v1/data-sources")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @GetMapping
    public R<List<DataSourceVO>> list(@RequestParam(defaultValue = "false") boolean onlyActive) {
        return R.ok(dataSourceService.list(onlyActive));
    }

    @GetMapping("/{id}")
    public R<DataSourceVO> get(@PathVariable Long id) {
        return R.ok(dataSourceService.get(id));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody DataSourceSaveDTO dto) {
        return R.ok(dataSourceService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody DataSourceSaveDTO dto) {
        dataSourceService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        dataSourceService.delete(id);
        return R.ok();
    }
}
