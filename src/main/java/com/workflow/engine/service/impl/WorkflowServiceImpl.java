package com.workflow.engine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workflow.engine.dto.WorkflowCreateRequest;
import com.workflow.engine.dto.WorkflowUpdateRequest;
import com.workflow.engine.exception.WorkflowNotFoundException;
import com.workflow.engine.mapper.WorkflowMapper;
import com.workflow.engine.model.Workflow;
import com.workflow.engine.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of WorkflowService.
 * Provides workflow management operations using MyBatis-Plus.
 */
@Slf4j
@Service
public class WorkflowServiceImpl extends ServiceImpl<WorkflowMapper, Workflow> implements WorkflowService {

    @Override
    @Transactional
    public Workflow create(WorkflowCreateRequest request) {
        log.info("Creating workflow with name: {}", request.name());

        Workflow workflow = new Workflow();
        workflow.setName(request.name());
        workflow.setDescription(request.description());
        workflow.setVersion(1);
        workflow.setStatus(Workflow.STATUS_DRAFT);

        save(workflow);

        log.info("Created workflow with id: {}", workflow.getId());
        return workflow;
    }

    @Override
    @Transactional
    public Workflow update(Long id, WorkflowUpdateRequest request) {
        log.info("Updating workflow with id: {}", id);

        Workflow workflow = getById(id);
        if (workflow == null) {
            throw new WorkflowNotFoundException(id);
        }

        workflow.setName(request.name());
        workflow.setDescription(request.description());

        updateById(workflow);

        log.info("Updated workflow with id: {}", id);
        return workflow;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Archiving workflow with id: {}", id);

        Workflow workflow = getById(id);
        if (workflow == null) {
            throw new WorkflowNotFoundException(id);
        }

        workflow.setStatus(Workflow.STATUS_ARCHIVED);
        updateById(workflow);

        log.info("Archived workflow with id: {}", id);
    }

    @Override
    public Workflow getById(Long id) {
        return super.getById(id);
    }

    @Override
    public List<Workflow> listAll() {
        return list();
    }
}