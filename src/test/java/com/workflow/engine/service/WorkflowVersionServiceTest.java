package com.workflow.engine.service;

import com.workflow.engine.dto.VersionSaveRequest;
import com.workflow.engine.dto.WorkflowCreateRequest;
import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.engine.model.WorkflowJSON;
import com.workflow.engine.exception.WorkflowNotFoundException;
import com.workflow.engine.exception.WorkflowVersionNotFoundException;
import com.workflow.engine.model.Workflow;
import com.workflow.engine.model.WorkflowVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowVersionService 集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkflowVersionServiceTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowVersionService workflowVersionService;

    private Workflow testWorkflow;

    @BeforeEach
    void setUp() {
        // 创建测试工作流
        WorkflowCreateRequest request = new WorkflowCreateRequest("测试工作流", "用于版本测试");
        testWorkflow = workflowService.create(request);
    }

    @Test
    void shouldSaveFirstVersion() {
        // Given: 准备画布数据
        WorkflowJSON canvas = createTestCanvas();
        VersionSaveRequest request = new VersionSaveRequest(canvas, "初始版本");

        // When: 保存版本
        WorkflowVersion version = workflowVersionService.saveVersion(testWorkflow.getId(), request);

        // Then: 验证版本信息
        assertNotNull(version.getId());
        assertEquals(1, version.getVersion());
        assertEquals(testWorkflow.getId(), version.getWorkflowId());
        assertEquals("初始版本", version.getChangeNote());
        assertNotNull(version.getCreatedAt());
    }

    @Test
    void shouldAutoIncrementVersionNumber() {
        // Given: 先保存一个版本
        WorkflowJSON canvas1 = createTestCanvas();
        workflowVersionService.saveVersion(testWorkflow.getId(), new VersionSaveRequest(canvas1, "v1"));

        // When: 保存第二个版本
        WorkflowJSON canvas2 = createTestCanvas();
        WorkflowVersion version2 = workflowVersionService.saveVersion(testWorkflow.getId(), new VersionSaveRequest(canvas2, "v2"));

        // Then: 版本号应为2
        assertEquals(2, version2.getVersion());
        assertEquals("v2", version2.getChangeNote());
    }

    @Test
    void shouldSaveVersionWithNullChangeNote() {
        // Given: 准备画布数据，变更说明为空
        WorkflowJSON canvas = createTestCanvas();
        VersionSaveRequest request = new VersionSaveRequest(canvas, null);

        // When: 保存版本
        WorkflowVersion version = workflowVersionService.saveVersion(testWorkflow.getId(), request);

        // Then: 验证版本信息
        assertNotNull(version.getId());
        assertEquals(1, version.getVersion());
        assertNull(version.getChangeNote());
    }

    @Test
    void shouldListVersionsInDescendingOrder() {
        // Given: 创建多个版本
        for (int i = 1; i <= 3; i++) {
            workflowVersionService.saveVersion(testWorkflow.getId(), new VersionSaveRequest(createTestCanvas(), "版本" + i));
        }

        // When: 获取版本列表
        List<WorkflowVersion> versions = workflowVersionService.listVersions(testWorkflow.getId());

        // Then: 版本号倒序排列
        assertEquals(3, versions.size());
        assertEquals(3, versions.get(0).getVersion());
        assertEquals(2, versions.get(1).getVersion());
        assertEquals(1, versions.get(2).getVersion());
    }

    @Test
    void shouldReturnEmptyListWhenNoVersions() {
        // When: 工作流没有版本时获取列表
        List<WorkflowVersion> versions = workflowVersionService.listVersions(testWorkflow.getId());

        // Then: 返回空列表
        assertNotNull(versions);
        assertTrue(versions.isEmpty());
    }

    @Test
    void shouldGetSpecificVersion() {
        // Given: 创建版本
        WorkflowJSON canvas = createTestCanvas();
        workflowVersionService.saveVersion(testWorkflow.getId(), new VersionSaveRequest(canvas, "v1"));

        // When: 获取版本
        WorkflowVersion version = workflowVersionService.getVersion(testWorkflow.getId(), 1);

        // Then: 验证返回正确版本
        assertEquals(1, version.getVersion());
        assertEquals("v1", version.getChangeNote());
    }

    @Test
    void shouldGetCorrectVersionWhenMultipleExist() {
        // Given: 创建多个版本
        for (int i = 1; i <= 3; i++) {
            workflowVersionService.saveVersion(testWorkflow.getId(), new VersionSaveRequest(createTestCanvas(), "版本" + i));
        }

        // When: 获取版本2
        WorkflowVersion version = workflowVersionService.getVersion(testWorkflow.getId(), 2);

        // Then: 验证返回正确版本
        assertEquals(2, version.getVersion());
        assertEquals("版本2", version.getChangeNote());
    }

    @Test
    void shouldThrowWhenVersionNotFound() {
        // When & Then: 获取不存在的版本应抛出异常
        assertThrows(WorkflowVersionNotFoundException.class, () -> {
            workflowVersionService.getVersion(testWorkflow.getId(), 999);
        });
    }

    @Test
    void shouldThrowWhenWorkflowNotFoundForSaveVersion() {
        // Given: 不存在的工作流ID
        WorkflowJSON canvas = createTestCanvas();
        VersionSaveRequest request = new VersionSaveRequest(canvas, "test");

        // When & Then: 保存版本应抛出异常
        assertThrows(WorkflowNotFoundException.class, () -> {
            workflowVersionService.saveVersion(999L, request);
        });
    }

    @Test
    void shouldThrowWhenWorkflowNotFoundForListVersions() {
        // When & Then: 获取不存在工作流的版本列表应抛出异常
        assertThrows(WorkflowNotFoundException.class, () -> {
            workflowVersionService.listVersions(999L);
        });
    }

    @Test
    void shouldThrowWhenWorkflowNotFoundForGetVersion() {
        // When & Then: 获取不存在工作流的版本应抛出异常
        assertThrows(WorkflowNotFoundException.class, () -> {
            workflowVersionService.getVersion(999L, 1);
        });
    }

    @Test
    void shouldThrowWhenWorkflowNotFoundForGetLatestVersion() {
        // When & Then: 获取不存在工作流的最新版本应抛出异常
        assertThrows(WorkflowNotFoundException.class, () -> {
            workflowVersionService.getLatestVersion(999L);
        });
    }

    @Test
    void shouldGetLatestVersion() {
        // Given: 创建多个版本
        for (int i = 1; i <= 3; i++) {
            workflowVersionService.saveVersion(testWorkflow.getId(), new VersionSaveRequest(createTestCanvas(), "版本" + i));
        }

        // When: 获取最新版本
        WorkflowVersion latestVersion = workflowVersionService.getLatestVersion(testWorkflow.getId());

        // Then: 最新版本应为版本3
        assertNotNull(latestVersion);
        assertEquals(3, latestVersion.getVersion());
        assertEquals("版本3", latestVersion.getChangeNote());
    }

    @Test
    void shouldReturnNullWhenNoLatestVersion() {
        // When: 工作流没有版本时获取最新版本
        WorkflowVersion latestVersion = workflowVersionService.getLatestVersion(testWorkflow.getId());

        // Then: 返回 null
        assertNull(latestVersion);
    }

    @Test
    void shouldSaveCanvasDataCorrectly() {
        // Given: 准备带有节点数据的画布
        NodeJSON startNode = new NodeJSON("start_0", "start", Map.of("title", "开始节点"), Map.of("x", 100, "y", 200));
        NodeJSON endNode = new NodeJSON("end_0", "end", Map.of("title", "结束节点"), Map.of("x", 300, "y", 200));
        WorkflowJSON canvas = new WorkflowJSON(List.of(startNode, endNode), List.of());

        // When: 保存版本
        VersionSaveRequest request = new VersionSaveRequest(canvas, "带节点数据的版本");
        WorkflowVersion version = workflowVersionService.saveVersion(testWorkflow.getId(), request);

        // Then: 验证画布数据保存正确
        assertNotNull(version.getCanvas());
        assertEquals(2, version.getCanvas().nodes().size());
        assertEquals("start_0", version.getCanvas().nodes().get(0).id());
        assertEquals("end_0", version.getCanvas().nodes().get(1).id());
    }

    /**
     * 创建测试用的画布数据
     */
    private WorkflowJSON createTestCanvas() {
        NodeJSON startNode = new NodeJSON("start_0", "start", Map.of(), Map.of("title", "开始"));
        NodeJSON endNode = new NodeJSON("end_0", "end", Map.of(), Map.of("title", "结束"));
        return new WorkflowJSON(List.of(startNode, endNode), List.of());
    }
}