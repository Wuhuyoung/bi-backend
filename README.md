# DataMaster 智能数据分析平台



## 项目介绍

基于 Spring Boot + MQ + AIGC 的智能数据分析平台。

区别于传统 BI，用户只需要导入原始数据集、并输入分析需求，就能自动生成可视化图表及分析结论，降低数据分析的人工成本、提高分析效率。

在线访问：http://bi.wuhuyoung.top



## 架构图

![image-20230812151315017](https://typora-1314662469.cos.ap-shanghai.myqcloud.com/img/202308121513880.png)



## 界面

首页：

![image-20230812134416204](https://typora-1314662469.cos.ap-shanghai.myqcloud.com/img/202308121411243.png)



智能分析（同步）：

![image-20230812140940351](https://typora-1314662469.cos.ap-shanghai.myqcloud.com/img/202308121411147.png)



智能分析（异步）：后台使用线程池异步提交，或使用消息队列处理

![image-20230812142310421](https://typora-1314662469.cos.ap-shanghai.myqcloud.com/img/202308121442568.png)



我的图表：

![image-20230812142346749](https://typora-1314662469.cos.ap-shanghai.myqcloud.com/img/202308121442348.png)



异步生成的分析可以手动刷新得到结果，生成失败的图表可以手动重试：

![image-20230812142927002](https://typora-1314662469.cos.ap-shanghai.myqcloud.com/img/202308121442927.png)





用户信息管理：统计用户生成图表的次数，采用积分系统，消耗积分来智能分析

![image-20230812143038248](https://typora-1314662469.cos.ap-shanghai.myqcloud.com/img/202308121442919.png)





## 技术选型

### 后端

- SpringBoot 2.7.2
- MySQL 8
- MyBatis-Plus 及 MyBatis X 自动生成
- Redis + Redisson 限流
- RabbitMQ 消息队列
- JDK 线程池
- EasyExcel 表格数据处理
- Swagger + Knife4j 接口文档

### 前端

- React
- Ant Design Pro 5.x脚手架
- Umi 4 前端框架
- Ant Design 组件库
- Echarts 可视化库



## 快速使用

1. 下载项目
   - 前端：https://github.com/Wuhuyoung/bi-frontend
   - 后端：https://github.com/Wuhuyoung/bi-backend
2. 启动前端
   1. npm install 下载依赖
   2. npm run dev 本地启动前端
3. 启动后端
   1. 导入后端项目中 sql 文件夹中 create_table.sql 的数据库表
   2. 修改 application.yml 中的数据库配置、Redis 配置、RabbitMQ 配置
   3. 启动 MainApplication.java



