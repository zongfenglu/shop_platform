package com.shopplatform.domain.shop.dns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Optional;

/**
 * 用 JDK JNDI DNS 查 CNAME。超时控制在 2 秒内，解析失败返回 empty 而不是抛给调用方。
 */
@Component
public class DnsCnameLookup implements CnameLookup {

    private static final Logger log = LoggerFactory.getLogger(DnsCnameLookup.class);

    @Override
    public Optional<String> lookupCname(String domain) {
        if (domain == null || domain.isBlank()) {
            return Optional.empty();
        }
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", "2000");
        env.put("com.sun.jndi.dns.timeout.retries", "1");
        try {
            InitialDirContext ctx = new InitialDirContext(env);
            try {
                Attributes attrs = ctx.getAttributes(domain, new String[]{"CNAME"});
                Attribute attr = attrs == null ? null : attrs.get("CNAME");
                if (attr == null || attr.size() == 0) {
                    return Optional.empty();
                }
                Object value = attr.get(0);
                return value == null ? Optional.empty() : Optional.of(String.valueOf(value));
            } finally {
                ctx.close();
            }
        } catch (NamingException e) {
            log.info("CNAME 查询失败 domain={} : {}", domain, e.getMessage());
            return Optional.empty();
        }
    }
}
