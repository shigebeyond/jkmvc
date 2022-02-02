<#list 0..9999 as i>
    Hello ${name}
    <#list friends as f>
        <#if i % 2 == 0>
            -${f}
        <#else>
            +${f}
        </#if>
    </#list>
</#list>
