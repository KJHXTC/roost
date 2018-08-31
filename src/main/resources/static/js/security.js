/*
 * 使用 CryptoJS 算法库
 * 使用 SHA-256 作为默认的安全策略,与后台的默认软实现逻辑相同
 * 如果希望采用其他方案,需要自行实现对应的客户端JS安全
 * 本文件内所有函数以驼峰法命名
 */

/**
 * 一种产生指定长度随机数的方法
 * <B>随机数质量以及安全性未知</B>
 */
function random(len){
  var b = new Uint8Array(len);
  for (var i=0; i< len; i++){
    b[i]= parseInt(Math.random()*1000) % 256;
  }
  var buff = '';
  b.forEach(function(x){
    buff += String.fromCharCode(x);
  });
  return window.btoa(buff);
}

function timestamp(){
  return new Date().getTime().toString().substring(0,10);
}

/**
 * 用服务器端的随机数和自身随机数 制作一个 空白票据
 * 然后 在票据上 增加 内容
 * 最后 用密码对需要的内容进行签名
 * <B>先计算服务端随机数,后计算客户端随机数</B>
 */
function generateTicket(server, client){
  var s = CryptoJS.enc.Base64.parse(server);
  var c = CryptoJS.enc.Base64.parse(client);
  return CryptoJS.SHA256(s.concat(c)).toString(CryptoJS.enc.Base64);
}

/**
 * 采用 AES/CFB/NoPadding 对 用户密码进行加密
 * 然后将密文送往后台
 * <B>注意:密码是明文hash后的结果
 * 意义: 某些日志中(例如Nginx的access.log)即使有记录但也不会明文出现客户密码
 */
function encryptPassword(kv, password){
  var block = CryptoJS.enc.Base64.parse(kv).words;
  var key = new CryptoJS.lib.WordArray.init(block.slice(0,4));
  var iv =  new CryptoJS.lib.WordArray.init(block.slice(4,8));
  var cfg={
      iv: iv,
      mode:CryptoJS.mode.CFB,
      padding:CryptoJS.pad.NoPadding
  };
  var hash = CryptoJS["SHA256"](password);
  return CryptoJS.AES.encrypt(hash, key,cfg).toString();
}

/**
 * 客户使用密码对 [经计算的 TOKEN(可能在网络中泄漏)]进行 HMac 作为认证票据
 * 因为后台存储的是客户的密码
 *   系统密钥 K AES e(内部唯一ID, 客户密码的哈希值 (不做任何加盐))
 *   Signature = HMac(key=hash(Password), message=元素数据)
 * 用户密码本身不会在网络中传输 (这里采用HMac方式对请求参数签名)
 * 需要包含时间戳(另外 客户端自身时间如果不正确,需要自行根据服务器的时间戳计算出当前的 UTC 时间)
 */
function signatureTicketWithHMac(token, content, password, method){
  if (typeof(method) == 'undefined'){
    method = 'SHA-256';
  }
  method = method.replace(' ','').replace('-','').toUpperCase();
  if (CryptoJS.hasOwnProperty(method) && CryptoJS.hasOwnProperty('Hmac'+method)){
    var key = CryptoJS["SHA256"](password); //注册时已经指明了SHA256加密
    var message = CryptoJS.enc.Base64.parse(token);
    if (null != content){
      message = message.concat(CryptoJS.enc.Utf8.parse(content));
    }
    return CryptoJS['Hmac'+method](message, key).toString(CryptoJS.enc.Base64);
  } else{
    throw new Error("Not Support Method:" + 'Hmac' + method);
  }
}
/**
 * 首先使用PBKDF2对用户的字符密码导出生成会话密钥
 * 因为后台存储的是哈希过的用户口令,因此不使用这种方式
 * salt 是服务器端动态生成的,因此会话密钥也会随之变化
 * 然后使用会话密钥对 TOKEN 进行HMac签名
 */
function signatureTicketWithPBKDF2(token, content, password, salt, iters, method){
  if (typeof(method) == 'undefined'){
    method = 'SHA-256';
  }
  if(typeof(iters) == 'undefined' || typeof(iters) != 'number') {
    iters = 10000;
  }
  method = method.replace(' ','').replace('-','').toUpperCase();
  var key = CryptoJS.PBKDF2(password, CryptoJS.enc.Base64.parse(salt), config);
  var config = {
    keySize: CryptoJS.algo[method].blockSize,
    hasher: CryptoJS.algo[method],
    iterations: iters
  };
  var message = CryptoJS.enc.Base64.parse(token);
  return CryptoJS['Hmac'+method](message, key).toString(CryptoJS.enc.Base64);
}

