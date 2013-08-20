import static org.ratpackframework.groovy.RatpackScript.ratpack

ratpack {
	handlers {
		get {
			redirect "index.html"
		}

		assets "public"
	}
}
