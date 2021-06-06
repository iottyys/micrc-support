/**
 * 应用服务(application service)支持库，应用服务由用例/功能驱动，包含在一个事务中
 * 推荐组织?
 * 应用服务表达为一个interface，使用领域模型作为参数和返回值，其中参数为单命令/事件对象，返回值为领域模型，其构建和转换(DTO)由端口处理
 * 实现逻辑?
 */
package io.ttyys.micrc.application;