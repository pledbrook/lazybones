def props = [:]

props.title = ask("What is the title of your presentation ?: ", "", "title")
props.revealjsTheme = ask("Which theme do you want to use [black]?: ", "black", "revealjsTheme")
props.author = ask("Who is the author [firstname lastname <email>]?: ", "", "author")

processTemplates('**/*', props)
