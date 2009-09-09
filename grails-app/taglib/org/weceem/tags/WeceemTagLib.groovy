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
package org.weceem.tags

import java.text.SimpleDateFormat
import org.weceem.controllers.ContentController
import org.codehaus.groovy.grails.commons.ApplicationHolder as AH
import org.weceem.content.Content
import org.weceem.services.ContentRepositoryService

class WeceemTagLib {
    
    static ATTR_ID = "id"
    static ATTR_NODE = "node"
    static ATTR_TYPE = "type"
    static ATTR_MAX = "max"
    static ATTR_SORT = "sort"
    static ATTR_ORDER = "order"
    static ATTR_OFFSET = "offset"
    static ATTR_PATH = "path"
    static ATTR_STATUS = "status"
    static ATTR_VAR = "var"
    static ATTR_CHANGEDSINCE = "changedSince"
    static ATTR_CHANGEDBEFORE = "changedBefore"
    static ATTR_CREATEDSINCE = "changedSince"
    static ATTR_CREATEDBEFORE = "changedBefore"
    static ATTR_FILTER = "filter"
    static ATTR_FORMAT = "format"
    static ATTR_CODEC = "codec"
    static ATTR_TITLE = "title"

    static namespace = "wcm"
    
    def contentRepositoryService
    def weceemSecurityService
    
    private extractCodec(attrs) {
        attrs[ATTR_CODEC] == null ? 'HTML' : attrs[ATTR_CODEC]        
    }
    
    private renderNodeProperty(propname, attrs) {
        def codec = extractCodec(attrs)
        out << request[ContentController.REQUEST_ATTRIBUTE_NODE].propname."encodeAs$codec"()
    }
    
    def space = { attrs -> 
        def codec = extractCodec(attrs)
        out << request[ContentController.REQUEST_ATTRIBUTE_SPACE].name."encodeAs$codec"()
    }
    
    /**
     * Tag that reveals user info while hiding the implementation details of the authentication system
     */
    def userInfo = { attrs, body -> 
        def user = weceemSecurityService.getUserPrincipal()
        def var = attrs[ATTR_VAR] ?: null
        out << body(var ? [(var):user] : user)
    }
    
/*
    def title = { attrs -> 
        renderNodeProperty('title', attrs)
    }

    def createdBy = { attrs -> 
        renderNodeProperty('createdBy', attrs)
    }

    def createdOn = { attrs -> 
        def codec = extractCodec(attrs)
        def format = attrs.format ?: 'yyyy/mm/dd hh:MM:ss'
        out << new SimpleDateFormat(format).format(request['activeNode'].createdOn)."encodeAs$codec"()
    }

    def changedBy = { attrs -> 
        renderNodeProperty('changedBy', attrs)
    }

    def changedOn = { attrs -> 
        def codec = extractCodec(attrs)
        def format = attrs[ATTR_FORMAT] ?: 'yyyy/mm/dd hh:MM:ss'
        out << new SimpleDateFormat(format).format(request['activeNode'].changedOn)."encodeAs$codec"()
    }
*/    
    private makeFindParams(attrs) {
        def r = [:]
        r.max = attrs[ATTR_MAX]
        r.offset = attrs[ATTR_OFFSET]
        r.sort = attrs[ATTR_SORT]
        r.order = attrs[ATTR_ORDER] ?: 'asc'
        r.changedSince = attrs[ATTR_CHANGEDSINCE]
        r.changedBefore = attrs[ATTR_CHANGEDBEFORE]
        r.createdSince = attrs[ATTR_CREATEDSINCE]
        r.createdBefore = attrs[ATTR_CREATEDBEFORE]
        return r
    }
    
    def eachChild = { attrs, body -> 
        def params = makeFindParams(attrs)
        if (attrs[ATTR_NODE] && attrs[ATTR_PATH]) {
          throwTagError("can not specify ${ATTR_NODE} and ${ATTR_PATH} attributes")
        }
        def baseNode = attrs[ATTR_NODE] ?: request[ContentController.REQUEST_ATTRIBUTE_NODE]
        def status = attrs[ATTR_STATUS] ?: ContentRepositoryService.STATUS_ANY_PUBLISHED
        if (attrs[ATTR_PATH]) {
            baseNode = contentRepositoryService.findContentForPath(attrs[ATTR_PATH], request[ContentController.REQUEST_ATTRIBUTE_SPACE]).content
        }
        def children = contentRepositoryService.findChildren(baseNode, [type:attrs[ATTR_TYPE], status:status, params:params])
        if (attrs[ATTR_FILTER]) children = children?.findAll(attrs[ATTR_FILTER])
        def var = attrs[ATTR_VAR] ?: null
        children?.each { child ->
            out << body(var ? [(var):child] : child)
        }
    }
    
    def countChildren = { attrs ->
        def baseNode = attrs[ATTR_NODE]
        if (!baseNode) {
            if (attrs[ATTR_PATH]) {
                baseNode = contentRepositoryService.findContentForPath(attrs[ATTR_PATH], request[ContentController.REQUEST_ATTRIBUTE_SPACE]).content 
            } else {
                baseNode = request[ContentController.REQUEST_ATTRIBUTE_NODE]
            }
        }
        def status = attrs[ATTR_STATUS] ?: ContentRepositoryService.STATUS_ANY_PUBLISHED
        out << contentRepositoryService.countChildren(baseNode, [type:attrs[ATTR_TYPE], status:status])
    }
    
    def eachParent = { attrs, body -> 
        def params = makeFindParams(attrs)
        def status = attrs[ATTR_STATUS] ?: ContentRepositoryService.STATUS_ANY_PUBLISHED
        def parents = contentRepositoryService.findParents(request[ContentController.REQUEST_ATTRIBUTE_NODE], 
            [type:attrs[ATTR_TYPE], status:status, params:params])
        if (attrs[ATTR_FILTER]) parents = parents?.findAll(attrs[ATTR_FILTER])
        def var = attrs[ATTR_VAR] ?: null
        parents?.each { parent ->
            out << body(var ? [(var):parent] : parent)
        }
    }
   
    def eachSibling = { attrs, body -> 
        def params = makeFindParams(attrs)
        def lineage = request[ContentController.REQUEST_ATTRIBUTE_PAGE].lineage
        def parentHierarchyNode = lineage.size() > 0 ? lineage[-1] : null
        def siblings 
        if (!parentHierarchyNode) {
            siblings = contentRepositoryService.findAllRootContent( 
                request[ContentController.REQUEST_ATTRIBUTE_SPACE],attrs[ATTR_TYPE])
        } else {
            siblings = contentRepositoryService.findChildren( parentHierarchyNode.parent, [type:attrs[ATTR_TYPE], params:params])
        }
        if (attrs[ATTR_FILTER]) siblings = siblings?.findAll(attrs[ATTR_FILTER])
        def var = attrs[ATTR_VAR] ?: null
        siblings?.each { sibling ->
            out << body(var ? [(var):sibling] : sibling)
        }
    }
    
    def eachDescendent = { attrs, body -> 
        throwTagError("eachDescendent not implemented yet")
    }
    
    def eachContent = { attrs, body -> 
        throwTagError("link not implemented yet")
        /*
        def params = makeFindParams(attrs)
        def content = contentRepositoryService.findContent(attrs[ATTR_TYPE], params)
        if (attrs[ATTR_FILTER]) content = content?.findAll(attrs[ATTR_FILTER])
        def var = attrs[ATTR_VAR] ?: null
        content?.each { node ->
            out << body(var ? [(var):node] : node)
        }
        */
    }
    
    def breadcrumb = { attrs -> 
        def node = request[ContentController.REQUEST_ATTRIBUTE_NODE]
        def lineage = request[ContentController.REQUEST_ATTRIBUTE_PAGE].lineage
        def div = attrs.divider ?: ' &gt; '
        def first = true
        def defaultBody = { n -> out << n.shortTitle.encodeAsHTML() } 
        
        lineage.each { parent ->
            // @todo this is probably bad, the service is transactional!
            if (parent != node) {
                if (!first) {
                    out << div
                }

                out << link(node:parent) {
                    defaultBody(parent)
                }
            }
            first = false
        }
    }
    
    def menu = { attrs ->
    }
    
    def link = { attrs, body -> 
        out << "<a href=\"${createLink(attrs)}\">"
        out << body()
        out << "</a>"
    }
    
    def createLink = { attrs, body -> 
        def space = request[ContentController.REQUEST_ATTRIBUTE_SPACE]
        def content = attrs[ATTR_NODE]
        if (content && !(content instanceof Content)) {
            throwTagError "Tag invoked with [$ATTR_NODE] attribute but the value is not a Content instance"
        }
        if (!content) {
            def contentInfo = contentRepositoryService.findContentForPath(attrs[ATTR_PATH], space)
            if (!contentInfo.content) {
                log.error ("Tag [wcm:createLink] cannot create a link to the content at path ${attrs[ATTR_PATH]} as "+
                    "there is no content node at that URI")
                out << g.createLink(controller:'content', action:'notFound', params:[path:attrs[ATTR_PATH]])
                return
            }
            content = contentInfo.content
        }
        
        // @todo This is quite crappy, we should be getting these urls from a cache
        StringBuffer path = new StringBuffer()
        if (space.aliasURI) {
            path << space.aliasURI
            path << '/'
        }
        if(content.absoluteURI) {
            path << content.absoluteURI
            path <<  '/'
        }
        attrs.params = [uri:path.toString()]
        attrs.controller = 'content'
        attrs.action = 'show'
        out << g.createLink(attrs)
    }
    
    def date = { attrs, body -> 
        out << formatDate(format:format ?: 'dd MMM yyyy', date: new Date())
    }
    
    def find = { attrs, body -> 
        def params = makeFindParams(attrs)
        def id = attrs[ATTR_ID]
        def title = attrs[ATTR_TITLE]
        def c
        if (id) {
            c = Content.get(id)
        } else if (title) {
            c = Content.findByTitle(title, params)
        } else throwTagError("One of [id] or [title] must be specified")
        def var = attrs[ATTR_VAR] ?: null
        out << body(var ? [(var):c] : c)
    }
    
    def content = { attrs, body ->
        // If null codec, default to HTML, but blank string codec means no encoding takes place
        def codec = attrs.codec
        def text = request[ContentController.REQUEST_ATTRIBUTE_NODE]?.content
        def content = codec ? text."encodeAs$codec"() : text 
        out << content
    }
    
    def createLinkToFile = { attrs ->
        def space = request[ContentController.REQUEST_ATTRIBUTE_SPACE]
        if (!attrs[ATTR_PATH]) {
            throwTagError("Attribute [${ATTR_PATH}] must be specified, eg the path to the file: images/icon.png")
        }
        out << g.resource(dir:"WeceemFiles/${space.name.encodeAsURL()}", file:attrs[ATTR_PATH])
    }

    def humanDate = { attrs ->
        def now = new Date()
        if (attrs.date) {
            use(org.codehaus.groovy.runtime.TimeCategory) {
                def millisDelta = now - attrs.date
                def daysElapsed = millisDelta.days
                if (daysElapsed > 0) {
                    out << message(code:'human.date.days.ago', args:[daysElapsed])
                } else {
                    def hoursElapsed = millisDelta.hours
                    if (hoursElapsed > 0) {
                        out << message(code:'human.date.hours.ago', args:[hoursElapsed])
                    } else {
                        def minutesElapsed = millisDelta.minutes
                        if (minutesElapsed > 0) {
                            out << message(code:'human.date.minutes.ago', args:[minutesElapsed])
                        } else {
                            def secondsElapsed = millisDelta.seconds
                            if (secondsElapsed > 0) {
                                out << message(code:'human.date.seconds.ago', args:[secondsElapsed])
                            }
                        }
                    }
                }
            }
        } else {
            out << message(code:'human.date.null')
        }
    }
    
    def loggedInUserName = { attrs ->
        out << weceemSecurityService.userName?.encodeAsHTML()
    }
    
    def ifUserCanEdit = { attrs, body ->
        def node = attrs[ATTR_NODE]
        if (!node) node = request[ContentController.REQUEST_ATTRIBUTE_NODE]

        if (weceemSecurityService.isUserAllowedToEditContent(node)) {
            out << body()
        }
    }
    
    def ifContentIs = { attrs, body ->
        def node = request[ContentController.REQUEST_ATTRIBUTE_NODE]
        def targetType = attrs[ATTR_TYPE]
        if (!targetType)
            throwTagError("Attribute [${ATTR_TYPE}] is required on tag ifContentIs. It must be a fully qualified class name")

        def targetClass = grailsApplication.getClassForName(targetType)
        if (!targetClass)
            throwTagError("Attribute [${ATTR_TYPE}] specified class [${targetType}] but it could not be located")
        
        if (targetClass.isAssignableFrom(node.class)) {
            out << body()
        }
    }

    def ifContentIsNot = { attrs ->
        def node = request[ContentController.REQUEST_ATTRIBUTE_NODE]
        def targetType = attrs[ATTR_TYPE]
        
        if (!grailsApplication.getClassForName(targetType).isAssignableFrom(node.class)) {
            out << body()
        }
    }
    
    def renderContentItemIcon = { attrs ->
        def type = attrs[ATTR_TYPE]
        def id = attrs[ATTR_ID]
        def iconconf = type.icon
        def pluginconf = AH.application.applicationMeta.find{it -> it.key == "plugins.${iconconf.plugin}"}
        def pluginContextPath
        if (pluginconf) { 
           pluginContextPath = "plugins/${iconconf.plugin}-${pluginconf.value}/${iconconf.dir}/"
        } else { 
           pluginContextPath = "${iconconf.dir}/"
        }
        out << "<div id='${id}' class='ui-content-icon'><img src='${g.resource(dir: pluginContextPath, file: iconconf.file)}'/></div>"
    }
}
