/*
 * 提供一种控制页面跳转和错误展示
 * 面向中文微信用户
 */

function twoFactorAuthMapper(o, kv){
  if( typeof(kv) == 'undefined'){
    var kv = 0;
  }
  var map ={
    "Authenticator": "动态口令",
    "EMAIL": "邮箱验证码",
    "SMS": "短信验证码",
    "E-TOKEN": "电子令牌"
  }

  var v = null;
  Object.entries(map).forEach(function (e) {
    if (e[0] == o) {
      if(kv < 2) {
        v = e[1];
      }
    } else if (e[1] == o) {
      if(kv != 1) {
        v = e[0];
      }
    }
  });
  return v;
}

function tokenPassword(password){
  return signatureTicketWithHMac(__TOKEN__, null, password, 'SHA-256');
}

function setToken(server, client){
  __TOKEN__ = generateTicket(server, client);
}

// 如果能仅限本JS内方法访问就好了 🙃
var __TOKEN__ = '';

function getLogin(that) {
  if($("#id_login").val().replace(' ','') == ''){
    $.toast('请输入用户名或手机号', 'cancel');
    return;
  }
  $(that).parent().hide();
  $(that).parent().next().show();
  var cSalt = random(16);
  var param = {
    action: "account",
    name: $("#id_login").val(),
    salt: cSalt,
  }
  var option = {
    200: function(data) {
      setToken(data.salt, cSalt);
      $.toast('请输入登录密码', 500);
      $("#vw_login_id").hide();
      $("#vw_login_pwd").show();
      $("#vw_login_2fa_select").hide();
      $("#vw_login_2fa").hide();
    },
    400: function(data) {
      $.toast('🙃无效的账户', 'cancel');
    },
    403: function(data) {
      $.toast('🔒账户已锁定', 'forbidden');
    },
    404: function(data) {
      $.toast('😭账户未找到', 'cancel');
    }
  }
  communicate(that, param, option);
}

//认证密码
function login(that) {
  if ($("#id_pwd").val().replace(' ','') == ''){
    $("#id_pwd").val('');
    $.toast('请输入密码', 'cancel');
    return;
  }
  $(that).parent().hide();
  $(that).parent().next().show();
  try{
    var param= {
      action: 'authorize',
      token: __TOKEN__,
      signature: tokenPassword($("#id_pwd").val())
    };
  } catch (err) {
    $.toptip("请联系管理员需求帮助#安全模块异常", 60000, 'error');
    return;
  }
  // 清空密码
  $("#id_pwd").val('');
  var option = {
    200: function(data){
      $("#vw_login_id").hide();
      $("#vw_login_pwd").hide();
      if(!data.enable2fa){
        bindResult(data);
      } else {
        $("#vw_login_2fa_select").show();
        $("#vw_login_2fa").hide();
        var selects = [];
        data.values.forEach(function (o){
          var s1 = twoFactorAuthMapper(o, 1);
          if (s1 != null) {
            selects.push(s1);
          }
        });
        $("#picker-2fa").picker({
          title: '请选择验证方式',
          cols: [
            {
             textAlign: 'center',
             values: selects
           }
          ]
        });
      }
    },
    401: function(data){
      $("#id_pwd").val('');
      $.toast('密码错误,请检查用户名和密码', 'cancel');
    }
  };
  var error = function(){
     $.toptip('操作失败,请检查输入后再试', 10000, 'error');
     $(that).parent().show();
     $(that).parent().next().hide();
  };
  communicate(that, param, option, error);
}

function picker2fa(that){
  if ($("#picker-2fa").val().replace(' ','') == ''){
    $.toast('请选择验证方式', 'cancel');
    return;
  }
  if(twoFactorAuthMapper($("#picker-2fa").val(), 2) == 'Authenticator'){
    $("#vw_login_id").hide();
    $("#vw_login_pwd").hide();
    $("#vw_login_2fa_select").hide();
    $("#vw_login_2fa").show();
    $(that).parent().show();
    $(that).parent().next().hide();
    return;
  }
  $(that).parent().hide();
  $(that).parent().next().show();
  var param = {
    action: 'send2code',
    token: __TOKEN__,
    auth: twoFactorAuthMapper($("#id_token").val(), 2)
  };
  var option = {
    200: function(data){
      $.notification({
        title: '发送成功',
        text: '验证码已成功发送,如果未收到点击这里重发',
        media: '',
        data: '',
        time: 60000,
        onClick: function(data) {
          $.closeNotification();
          $.showLoading();
          picker2fa(that);
        },
        onClose: function(data) {
          setTimeout(function() {
            $.hideLoading();
          }, 3000);
        }
      });
      $("#vw_login_id").hide();
      $("#vw_login_pwd").hide();
      $("#vw_login_2fa_select").hide();
      $("#vw_login_2fa").show();
      $(that).parent().show();
      $(that).parent().next().hide();
    }
  };
  var error= function(){
    $.toptip('验证码发送失败,请稍后再试', 10000, 'error');
  }
  communicate(that, param, option, error)
}

function auth2FA(that) {
  if ($("#id_token").val().replace(' ','') == ''){
    $.toast('请输入验证码', 'cancel');
    return;
  }
  $(that).parent().hide();
  $(that).parent().next().show();
  var param = {
    action: "auth2fa",
    token: __TOKEN__,
    code: $("#id_token").val()
  };
  var option = {
    200: function(data){
      $("#vw_login_id").hide();
      $("#vw_login_pwd").hide();
      $("#vw_login_2fa_select").hide();
      $("#vw_login_2fa").hide();
      bindResult(data);
    },
    401: function(data){
      $("#vw_login_id").hide();
      $("#vw_login_pwd").hide();
      $("#vw_login_2fa_select").show();
      $("#vw_login_2fa").hide();
      $("#id_token").val('');
      $.toast('验证码不正确', 'cancel');
    }
  };
  communicate(that, param, option);
}

function bindResult(data){
  $("#vw_bind_option").hide();
  $("#vw_bind_result").show();
  $("#id_result_title").text(data.title);
  $("#id_result_msg").text(data.message);
}

function communicate(that, param, ready, error){
  param.date = new Date().toJSON();
  $.ajax({
    url: '/accounts/bind',
    type: 'GET',
    async: true,
    dataType: "json",
    data: param,
    success: function(data) {
      if(data.status == 200) {
        console.log(data);
        if(typeof(ready[data.status]) == 'function'){
          ready[data.status](data);
        } else{
          $.toast('操作成功', 1000);
        }
      } else {
        $(that).parent().show();
        $(that).parent().next().hide();
        if(typeof(ready[data.status]) == 'function'){
          // 注册的 400 401 403 404 错误处理
          ready[data.status](data);
        } else {
          // 超出预期的 异常错误码
          $.toast(data.message, 'cancel');
          $.toptip(data.message, 10000, 'error');
          console.error("failed", data);
        }
      }
    },
    error: function(){
      $.toptip('操作失败,请稍后再试', 10000, 'error');
      setTimeout(function(){
        $(that).parent().show();
        $(that).parent().next().hide();
      }, 10000);
      if(typeof(error) == 'function'){
        error();
      }
    }
  });
}