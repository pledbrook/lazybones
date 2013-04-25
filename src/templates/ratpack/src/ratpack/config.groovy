import org.ratpackframework.groovy.config.Config


(this as Config).with {

	routing.with {
		reloadable = true

		// Uncomment this to allow for statically compiled Groovy files.
//		routing.staticallyCompile = true
	}

}
