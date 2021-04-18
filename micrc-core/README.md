# micrc-core
## 低代码所需的各支持功能及使用的技术框架扩展
1. 服务集成支持，根据协议生成接口的集成注解，使用springboot动态代理注入使用，支持本地同步调用，本地同步/异步消息发送，远程同步调用，远程异步消息发送几个produce功能。内部使用camel构建集成路由，完成通信。
2. 数据转换支持，扩展camel component，实现mapstruct协议，以支持数据转换逻辑及assembler模式的集成