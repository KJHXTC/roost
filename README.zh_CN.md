# 微信公众号后台服务

## 说明

### 功能说明
本项目预置的服务功能 属于基础的服务管理功能,旨在提供一种助手式服务



com.kjhxtc.

## 许可协议
为了保证源代码可以自由使用 修改,但禁止二次**公开发版**
欢迎提出Issue(包括但不限于 缺陷 功能 疑问以及代码优化和其他意见)
欢迎 Pull Request
## 许可引用


## 结构说明


## RESTFul 国际化策略

HTTP 发起的RESTful 接口调用 
   如果是从浏览器发起的 (例如AJAX)常规有如下Header
   * Cookies
   * Referer
   * User-Agent
   * Accept-Language
      
   提取用户语言优先级 
   1. 如果 Cookie 中 存在 则从 优先执行 Cookie 的语言设定(用户最佳偏好)
   2. 如果 是可鉴别的用户 则 从
   3. 如果 QueryString 中`hl` [Google]存在且是有效的, 则执行 `hl` 设定
   4. 如果从 Accept-Language 提取
   5. 如果都没有, 则从服务端的默认中提取
   将用户语言优先级按照 提取结果排序
   
   国际化翻译,
   按照优先级的语言进行翻译,否则执行系统策略
   
  
  
  思考:
    基于模板的国际化可以减少复杂度,但对一些操作习惯 语言习惯等不同甚至相反的使用者来讲并不能被认可
    
    WEB服务应尽可能国际化(统一服务),但客户端(Desktop,APP)服务应尽可能的本地化(个性化服务)
    
    
## 动态的 WEB 服务
  一般来讲WEB(HTTP)服务需要如下的
  HTTP              Port 80   默认
  HTTP over TLS/SSL Port 443  默认
  主机 Host [DomainName:Port]
  
  先来回顾下
  而客户端在发起请求是 由于需要Socket 所以 会优先解析 
  Domain(FQDN)  ----DNS query ----> 最终 得到 IPAddress [IPv4 |IPv6]
  然后如果 自定义了端口 则使用端口 如果没有则使用默认端口
  在得到 IPAddress 和 Port 后 发起TCP连接
  连接成功后 
     如果是 HTTPS 由于多了一层 TLS/SSL 故 需要 进行安全认证 这里的参见 [](),完成后下一步
  然后
    发送 HTTP请求 {方法, 版本, 主机, Header, URI, 参数}
    
  好了,那么来看服务器如何解析查找对应的处理呢
  在被连接后(如果是TLS/SSL则是握手认证完成后)
  收到HTTP的请求
      提取 Host
      找到绑定的 Host 转发到 处理函数, 
      处理 根据 URI 规则 (路由规则) 找到 处理器
      
      处理器 根据 请求的 HTTP 方法 [GET | POST | DELETE ...} 执行对应的 函数
      函数 执行出 结果(页面/数据/异常 经上层进行数据渲染后 写入Socket)
      
     * 中间件
     * WEB服务器 静态资源 动态资源 nginx 反向代理
  另外了解下 **工具** 和 **框架** 的区别
  有兴趣的可以看下 tornado webServer 的 helloword 页面
  就需要这些 
    1. 注册 Host 
    2. 注册 路由  "/hello" -> helloWord.get()
    3. 实现 实现控制器 的方法 GET POST ...
    

  JAVA(java 派生的JVM系列语言) 因为有 JavaEE的规范标准化的框架因此 不少中间件都已实现和集成了基础的服务
  不需要你重复造轮子了(最简单的 tomcat 实现静态资源路由,动态的Http解析(http报文->java class服务))
  但用惯了python的(Flask Django  Tornado Web Web2 ...)的反而觉得用起来也是不舒服,有规则就有约束
  