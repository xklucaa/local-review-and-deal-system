package com.local_review_deal_sys.utils;

import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.UUID;

// md5 本身是不可逆的，只不过可以用彩虹表查询，因为同一个字符串用md5加密后生成的密码是固定的
// 密码工具类
// 加盐加密 / 加盐解密
public class PasswordTools {
    /**
     *   加盐加密
     * @param password   明文密码
     * @return           加盐加密的密码
     */
    public static String encrypt(String password) {
        // 1、产生盐值  此处产生去掉-后 32 位的盐值
        String salt = UUID.randomUUID().toString().replace("-","");
        System.out.println("盐值：" + salt);
        // 2、使用（盐值+明文密码）得到加密的密码
        // getBytes() 括号里设置编码 因为盐值没有中文，所以这里无需设置utf8
        // 此处加密后的密码是 32位
        String finalPassword = DigestUtils.md5DigestAsHex((salt + password).getBytes());
        // 3、将盐值和加密的密码共同返回（合并盐值和加密密码）
        // 总共 65 位
        String dbPassword = salt + "$" + finalPassword;
        return dbPassword;
    }

    /**
     *  接收传入的盐值进行加密
     * @param password  明文密码
     * @param salt      盐值
     * @return          加盐加密的密码
     */
    public static String encrypt(String password,String salt) {
        // 1、使用（盐值+明文密码）得到加密的密码
        // getBytes() 括号里设置编码 因为盐值没有中文，所以这里无需设置utf8
        // 此处加密后的密码是 32位
        String finalPassword = DigestUtils.md5DigestAsHex((salt + password).getBytes());
        // 2、将盐值和加密的密码共同返回（合并盐值和加密密码）
        // 总共 65 位
        String dbPassword = salt + "$" + finalPassword;
        return dbPassword;
    }

    /**
     * 解密(对比密码是否正确)
     * @param password    明文密码（不一定对，需要验证明文密码）
     * @param dbPassword  数据库存储的密码（包含：salt+$+加盐加密密码）
     * @return       true=密码正确
     */
    public static boolean decrypt(String password, String dbPassword){
        boolean result = false;
        if(StringUtils.hasLength(password) && StringUtils.hasLength(dbPassword) &&
                dbPassword.length() == 65 && dbPassword.contains("$")) {  // 参数正确
            // 1、得到盐值
            String[] passwordArr = dbPassword.split("\\$");
            // 1.1、盐值
            String salt = passwordArr[0];
//            // 1.2、得到正确密码的加盐加密密码
//            String finalPassword = passwordArr[1];
            // 2、生成验证密码的加盐加密密码
            String checkPassword = encrypt(password, salt);
            if (dbPassword.equals(checkPassword)) {
                result = true;
            }
        }
        return result;
    }
    //    // 测试
    public static void main(String[] args) {
        String password = "123456";

        String dbpwd1 = PasswordTools.encrypt(password);
        String dbpwd2 = PasswordTools.encrypt(password);
        String dbpwd3 = PasswordTools.encrypt(password);

        System.out.println(dbpwd1);
        System.out.println(dbpwd2);
        System.out.println(dbpwd3);

        String cPwd = "12345";
        boolean result = PasswordTools.decrypt(cPwd, dbpwd1);
        System.out.println("错误对比结果 ->" + result);

        boolean result2 = PasswordTools.decrypt(password, dbpwd2);
        System.out.println("正确对比结果 ->" + result2);
    }
}
