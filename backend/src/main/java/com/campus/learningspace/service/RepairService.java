package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.Classroom;
import com.campus.learningspace.entity.Repair;
import com.campus.learningspace.entity.RepairVO;
import com.campus.learningspace.mapper.RepairMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepairService extends ServiceImpl<RepairMapper, Repair> {

    @Autowired
    private ClassroomService classroomService;

    public List<RepairVO> getUserRepairVOList(Long userId) {
        return baseMapper.selectUserRepairVOList(userId);
    }

    public RepairVO getRepairVOById(Long id) {
        return baseMapper.selectRepairVOById(id);
    }

    public List<Repair> getUserRepairs(Long userId) {
        LambdaQueryWrapper<Repair> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Repair::getUserId, userId)
                .orderByDesc(Repair::getCreateTime);
        return list(wrapper);
    }

    public List<Repair> getPendingRepairs() {
        LambdaQueryWrapper<Repair> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Repair::getStatus, 1, 2); // 1-待处理, 2-处理中
        return list(wrapper);
    }

    /**
     * 根据报修状态同步教室实时状态：
     * - 报修进入处理中(2) => 教室 realTimeStatus=2（维修中）
     * - 报修离开处理中且无其他处理中工单 => 教室 realTimeStatus=1（可用）
     */
    public void syncClassroomRealtimeStatusByRepair(Long repairId, Long classroomId, Integer newRepairStatus) {
        if (classroomId == null) return;
        Classroom classroom = classroomService.getById(classroomId);
        if (classroom == null || (classroom.getDeleted() != null && classroom.getDeleted() == 1)) return;

        if (newRepairStatus != null && newRepairStatus == 2) {
            if (classroom.getRealTimeStatus() == null || classroom.getRealTimeStatus() != 2) {
                classroom.setRealTimeStatus(2);
                classroomService.updateById(classroom);
            }
            return;
        }

        // 当前工单不再“处理中”时，若不存在其他处理中工单，恢复可用
        long processingCount = lambdaQuery()
                .eq(Repair::getResourceType, 1)
                .eq(Repair::getClassroomId, classroomId)
                .eq(Repair::getStatus, 2)
                .eq(Repair::getDeleted, 0)
                .ne(repairId != null, Repair::getId, repairId)
                .count();
        if (processingCount == 0 && (classroom.getRealTimeStatus() == null || classroom.getRealTimeStatus() != 1)) {
            classroom.setRealTimeStatus(1);
            classroomService.updateById(classroom);
        }
    }
}

