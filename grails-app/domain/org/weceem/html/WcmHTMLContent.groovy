package org.weceem.html

import org.weceem.content.*

import org.weceem.util.ContentUtils
import org.weceem.content.TemplateUtils

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * WcmHTMLContent class describes the content node of type 'HTML'.
 *
 * @author Stephan Albers
 * @author July Karpey
 * @author Marc Palmer
 * @author Viktor Fedorov
 */
class WcmHTMLContent extends WcmContent {

    static searchable = {
        alias WcmHTMLContent.name.replaceAll("\\.", '_')
        only = ['content', 'keywords', 'htmlTitle', 'menuTitle', 'title', 'status']
    }
    
    Boolean allowGSP = false
    
    String keywords
    WcmTemplate template

    // 64Kb Unicode text with HTML/Wiki Markup
    String content
    String menuTitle
    String htmlTitle

    @Override
    String getHardDependencies() {
        // A template is an implicit dependency for the node, any changes to the template or its deps
        // means we have to change too.
        def t = TemplateUtils.getTemplateForContent(this)
        return t ? t.absoluteURI : ''
    }

    /**
     * Must be overriden by content types that can represent their content as text.
     * Used for search results and versioning
     */
    public String getContentAsText() { ContentUtils.htmlToText(content) }

    /**
     * Should be overriden by content types that can represent their content as HTML.
     * Used for wcm:content tag (content rendering)
     */
    public String getContentAsHTML() { content }
    
    Map getVersioningProperties() { 
        def r = super.getVersioningProperties() + [ 
            menuTitle:menuTitle,
            htmlTitle:htmlTitle,
            keywords:keywords,
            template:template?.ident() // Is this right?
        ] 
        return r
    }
    
    String getMimeType() { "text/html; charset=UTF-8" }
    
    static handleRequest = { content ->
        if (content.allowGSP) {
            renderGSPContent(content)
        } else {
            renderContent(content)
        }
    }
    
    static constraints = {
        content(nullable: false, maxSize: WcmContent.MAX_CONTENT_SIZE)
        keywords(nullable: true, blank: true, maxSize: 200)
        menuTitle(nullable: true, blank: true, maxSize: 40)
        htmlTitle(nullable: true, blank: true, maxSize: 400)
        allowGSP(nullable: true)
        template(nullable: true)
        status(nullable: false) // Workaround for Grails 1.1.1 constraint inheritance bug
    }

    static mapping = {
        cache usage:'read-write', include: 'non-lazy'
        template cascade: 'all', lazy: false // we never want proxies for this
        columns {
            content type:'text'
            htmlTitle type:'text'
        }
    }

    static editors = {
        template(group:'extra')
        menuTitle(group:'extra')
        htmlTitle(group:'extra')
        content(editor:'HTMLContent')
        keywords(group:'meta')
    }

    static transients = WcmContent.transients + [ 'summary']

    static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "html-file-32.png"]

    /**
     * Overriden to return caption for menu items, if supplied
     */
    public String getTitleForMenu() { menuTitle ?: title }

    /**
     * Overriden to return caption for menu items, if supplied
     */
    public String getTitleForHTML() { htmlTitle ?: title }

    public String getSummary() {
        def summaryString = ""
        def parts = content.split("<")
        parts.each() {
            def modified = it.substring(it.indexOf(">") + 1)
            if (modified) summaryString += modified
        }

        if (content && summaryString.length() >= 100) {
            summaryString = summaryString.substring(0, 99)
            summaryString = summaryString.substring(0, summaryString.lastIndexOf(" "))
        }

        return summaryString
    }

}