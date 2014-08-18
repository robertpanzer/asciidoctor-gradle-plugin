package org.asciidoctor.gradle;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger

public class AsciidoctorExtensionsDelegate {

	private AsciidoctorTask task

	private List blockProcessors = []

	private static AtomicInteger extensionClassCounter = new AtomicInteger(0)
	
	AsciidoctorExtensionsDelegate(AsciidoctorTask task) {
		this.task = task
	}
	
	void configure(Closure cl) {
		cl.delegate = this
		cl.run()
	}
	
	
	void registerExtensions(Object javaExtensionRegistry, GroovyClassLoader groovyClassLoader) {
		blockProcessors.each {
			blockProcessor ->
			javaExtensionRegistry.block(createBlockProcessor(blockProcessor[0], blockProcessor[1], groovyClassLoader))
		}
	}

    // Everything got BlockProcessors

	Object createBlockProcessor(Map options, Closure cl, GroovyClassLoader groovyClassLoader) {
		
		def simpleClassName = "Extension_" + extensionClassCounter.getAndIncrement()
		def packageName = "org.asciidoctor.gradle.extensions"
		def classContent = """package $packageName
import org.asciidoctor.ast.AbstractBlock;
import org.asciidoctor.extension.Reader;
class $simpleClassName extends org.asciidoctor.extension.BlockProcessor {

		private Closure cl

		public $simpleClassName(String name, Map options, Closure cl) {
		    super(name, options)
			this.cl = cl
		}

		public Object process(AbstractBlock parent, Reader reader, Map attributes) {
			cl.call(parent, reader, attributes)
		}
}"""
		
		Class clazz = groovyClassLoader.parseClass(classContent)
		//Map<String, Object> options = ["contexts": [":open", ":paragraph"], "content_model": ":simple"]
		Object processor = clazz.newInstance(options["name"], options, cl)
		cl.delegate = processor
		return processor
	}
	
	void block(Map options=[:], Closure cl) {
        if (!options.containsKey("contexts")) {
            options["contexts"] = [":open", ":paragraph"]
        }
		blockProcessors << [options, cl]
	}

    void block(String blockName, Closure cl) {
        block(["name": blockName], cl)
    }

    // Everything for other processors

}
