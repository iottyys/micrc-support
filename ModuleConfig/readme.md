首先清理:
  1. gradle中ModuleConfig module下
    1.1 build组-clean任务执行
    1.2 protocol generation组-clearAvroProtocol任务执行
开始编译
  2. avdl-avpr(协议注册中心插件 sad-gradle-plugin 做):
    2.1 protocol generation组-designTechnology任务执行
  3. 模块插件 codegen-gradle-plugin 做:
    3.1 build组-build任务执行
完成后:
    项目ModuleConfig
       模块auth下:
         1. ModuleConfig/src/main/avro/feature/auth                 生成的avpr文件目录
         2. ModuleConfig/auth/build/generated-main-avro-java        生成的接口文件目录
         3. ModuleConfig/auth/build/generated/sources/annotationProcessor/java/main
                                                                    生成的具体实现类的文件目录
