
function encrypt_password(password){
  var kv = CryptoJS.enc.Base64.parse($("#cipher_key").val()).words;
  var key = new CryptoJS.lib.WordArray.init(kv.slice(0,4));
  var iv = new CryptoJS.lib.WordArray.init(kv.slice(4,8));
  var cfg={
      iv: iv,
      mode:CryptoJS.mode.CFB,
      padding:CryptoJS.pad.NoPadding
  };
  var hash = CryptoJS["SHA256"](password)
  return CryptoJS.AES.encrypt(hash, key,cfg).toString();
}