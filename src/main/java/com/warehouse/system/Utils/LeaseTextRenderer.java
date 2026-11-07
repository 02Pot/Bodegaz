package com.warehouse.system.Utils;

import com.warehouse.system.DTO.Lease.LeaseClause;
import com.warehouse.system.DTO.Lease.LeaseTermsSnapshot;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LeaseTextRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-z_]+)\\s*}}");

    public String render(LeaseTermsSnapshot snapshot) {
        Map<String, String> v = snapshot.placeholders();
        StringBuilder sb = new StringBuilder();

        sb.append(fill("Agreement No.: {{lease_number}}\nDate: {{agreement_date}}\n\n", v));
        sb.append(fill("Lessor: {{company_name}}\n"
                + "Tenant: {{tenant_name}}\n\n", v));

        int n = 1;
        for (LeaseClause clause : snapshot.clauses()) {
            sb.append(n++).append(". ").append(clause.title()).append(". ")
                    .append(fill(clause.body(), v)).append("\n\n");
        }
        return sb.toString().strip();
    }

    private String fill(String text, Map<String, String> values) {
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        Set<String> missing = new TreeSet<>();
        while (m.find()) {
            String value = values.get(m.group(1));
            if (value == null) {
                missing.add(m.group(1));
                m.appendReplacement(out, "");
            } else {
                String clean = value.replace("**", "").strip();
                String bolded = clean.isEmpty() ? "" : "**" + clean + "**" + (value.endsWith(" ") ? " " : "");
                m.appendReplacement(out, Matcher.quoteReplacement(bolded));
            }
        }
        m.appendTail(out);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Lease template uses unknown placeholders: " + missing);
        }
        return out.toString();
    }
}