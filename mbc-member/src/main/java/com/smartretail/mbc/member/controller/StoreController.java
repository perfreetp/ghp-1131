package com.smartretail.mbc.member.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartretail.mbc.common.result.Result;
import com.smartretail.mbc.member.dto.StoreQueryDTO;
import com.smartretail.mbc.member.dto.StoreTaskHandleDTO;
import com.smartretail.mbc.member.dto.StoreTaskQueryDTO;
import com.smartretail.mbc.member.service.StoreService;
import com.smartretail.mbc.member.service.StoreTaskService;
import com.smartretail.mbc.member.vo.StoreTaskBoardVO;
import com.smartretail.mbc.member.vo.StoreVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "门店管理模块")
@RestController
@RequestMapping("/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    private final StoreTaskService storeTaskService;

    @Operation(summary = "根据ID查询门店", description = "根据门店ID查询门店详情信息")
    @GetMapping("/{id}")
    public Result<StoreVO> getById(
            @Parameter(description = "门店ID", required = true)
            @PathVariable("id") Long id) {
        return Result.success(storeService.getById(id));
    }

    @Operation(summary = "根据编码查询门店", description = "通过门店编码查询门店信息")
    @GetMapping("/by-code/{storeCode}")
    public Result<StoreVO> getByCode(
            @Parameter(description = "门店编码", required = true)
            @PathVariable("storeCode") String storeCode) {
        return Result.success(storeService.getByCode(storeCode));
    }

    @Operation(summary = "查询所有启用门店", description = "查询所有状态为启用的门店列表")
    @GetMapping("/list-all")
    public Result<List<StoreVO>> listAll() {
        return Result.success(storeService.listAll());
    }

    @Operation(summary = "分页查询门店列表", description = "按条件分页查询门店列表")
    @PostMapping("/page")
    public Result<IPage<StoreVO>> pageQuery(
            @Parameter(description = "查询条件", required = true)
            @RequestBody StoreQueryDTO dto) {
        return Result.success(storeService.page(dto));
    }

    @Operation(summary = "新增门店", description = "创建新门店，门店编码唯一")
    @PostMapping("/create")
    public Result<StoreVO> create(
            @Parameter(description = "门店信息", required = true)
            @Valid @RequestBody StoreVO vo) {
        return Result.success(storeService.create(vo));
    }

    @Operation(summary = "更新门店信息", description = "更新门店基本信息")
    @PutMapping("/update")
    public Result<StoreVO> update(
            @Parameter(description = "门店信息", required = true)
            @Valid @RequestBody StoreVO vo) {
        return Result.success(storeService.update(vo));
    }

    @Operation(summary = "删除门店", description = "根据ID删除门店")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "门店ID", required = true)
            @PathVariable("id") Long id) {
        storeService.delete(id);
        return Result.success();
    }

    @Operation(summary = "门店任务看板", description = "获取门店任务看板总览，含待处理数、高优先级数、各类型统计和任务列表")
    @PostMapping("/task/board")
    public Result<StoreTaskBoardVO> getTaskBoard(
            @Parameter(description = "查询条件", required = true)
            @RequestBody StoreTaskQueryDTO dto) {
        return Result.success(storeTaskService.getStoreTaskBoard(dto));
    }

    @Operation(summary = "处理任务", description = "处理门店任务，支持确认处理、标记忽略、转交总部")
    @PostMapping("/task/handle")
    public Result<Void> handleTask(
            @Parameter(description = "任务处理请求", required = true)
            @Valid @RequestBody StoreTaskHandleDTO dto) {
        storeTaskService.handleTask(dto);
        return Result.success();
    }

    @Operation(summary = "生成当日任务", description = "手动触发生成门店当日任务（定时任务也会自动生成）")
    @PostMapping("/task/generate")
    public Result<Void> generateDailyTasks(
            @Parameter(description = "门店编码", required = true)
            @RequestParam String storeCode,
            @Parameter(description = "任务日期")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        storeTaskService.generateDailyTasks(storeCode, date);
        return Result.success();
    }
}
