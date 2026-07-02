package com.hoho.kb.controller;

import java.util.List;

import com.hoho.common.core.domain.R;
import com.hoho.kb.domain.Qa;
import com.hoho.kb.model.request.QaRequest;
import com.hoho.kb.service.QaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 问答知识接口
 *
 * @author hoho
 */
@RestController
@RequestMapping("/kb/qa")
public class QaController
{
    private static final Logger log = LoggerFactory.getLogger(QaController.class);

    private final QaService qaService;

    public QaController(QaService qaService)
    {
        this.qaService = qaService;
    }

    @PostMapping
    public R<Long> create(@RequestBody QaRequest request)
    {
        try
        {
            return R.ok(qaService.create(request));
        }
        catch (Exception e)
        {
            log.error("创建问答知识失败", e);
            return R.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody QaRequest request)
    {
        try
        {
            qaService.update(id, request);
            return R.ok(true);
        }
        catch (Exception e)
        {
            log.error("更新问答知识失败", e);
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public R<Qa> get(@PathVariable Long id)
    {
        try
        {
            return R.ok(qaService.get(id));
        }
        catch (Exception e)
        {
            log.error("查询问答知识失败", e);
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/list")
    public R<List<Qa>> list()
    {
        return R.ok(qaService.list());
    }

    @PostMapping("/{id}/publish")
    public R<Boolean> publish(@PathVariable Long id)
    {
        try
        {
            qaService.publish(id);
            return R.ok(true);
        }
        catch (Exception e)
        {
            log.error("发布问答知识失败", e);
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/offline")
    public R<Boolean> offline(@PathVariable Long id)
    {
        try
        {
            qaService.offline(id);
            return R.ok(true);
        }
        catch (Exception e)
        {
            log.error("下线问答知识失败", e);
            return R.fail(e.getMessage());
        }
    }
}
