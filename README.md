[![Build Status](https://travis-ci.org/blueshen/ik-analyzer.svg)](https://travis-ci.org/blueshen/ik-analyzer)

Long Term Support，welcome pull request

IKAnalyzer的作者为林良益（linliangyi2007@gmail.com），项目网站为<http://code.google.com/p/ik-analyzer/>

本版本，主要特点：

- Maven化, 代码格式化
- 添加了不少停用词，使其适用于中文分词
- 全面支持的lucene全系列版本为5/6/7/8, 持续技术支持 
- 支持加载远程扩展词库，可通过构造方法传入远程词库地址 new IKAnalyzer(useSmart, romoteExtDict)

Maven用法：

将以下依赖加入工程的pom.xml中的依赖部分。

```xml
    <dependency>
        <groupId>org.wltea.ik-analyzer</groupId>
        <artifactId>ik-analyzer</artifactId>
        <version>8.1.0</version>
    </dependency>
```
在IK Analyzer加入Maven Central Repository之前，你需要手动安装，安装到本地的repository，或者上传到自己的Maven repository服务器上。

要安装到本地Maven repository，使用如下命令，将自动编译，打包并安装：

```shell
    mvn clean install -Dmaven.test.skip=true
```

