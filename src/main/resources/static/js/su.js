/**
 * 对密码框的密码进行对称算法加密保护
 */
function encrypt_password(){
  var kv = $("#cipher_key").val();
  var password = $("#password_id").val();
  return encryptPassword(kv, password);
}
/**
 * 使用用户密码对元素签名,以证明用户身份
 * 此种方案是建立在 用户密码可被还原的基础上的
 */
function signature_token(){
  var password = $("#password_id").val();
  var server_r = $("#id_server_random").val();
  var server_t = $("#id_server_timestamp").val();
  var client_r = $("#id_login_token").val(random(16)).val();
  var client_t = $("#id_login_time").val(timestamp()).val();
  var delta_rc = $("#id_login_delta").val(client_t - server_t).val();

  var token = generateTicket(server_r, client_r);
  var content = (server_t + client_t + delta_rc);
  return signatureTicketWithHMac(token, content, password);
}