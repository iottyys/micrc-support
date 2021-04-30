方案设计目录 program
    模块目录
        需求用例的协议定义
        模块间协议定义映射
            relation.json       主要描述接口应该被哪些模块调用
            同步调用
            异步消息
    module.json
      主要填入:
        packagePrefix: 包前缀,
        deploymentMethod: 部署方式(
          单jar部署single(后期请求用的是本地调用的方式),
          多jar部署multiple(后期请求用的是远程调用方式)
        )
    
功能设计目录 feature
