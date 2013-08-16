import static org.ratpackframework.groovy.RatpackScript.ratpack
import static org.ratpackframework.groovy.Template.groovyTemplate

ratpack {
    handlers {
        get {
            render groovyTemplate("index.html", title: "My Ratpack App")
        }
        
        assets "public"
    }
}
