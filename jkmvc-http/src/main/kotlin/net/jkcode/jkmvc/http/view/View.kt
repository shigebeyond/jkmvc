package net.jkcode.jkmvc.http.view

import net.jkcode.jkmvc.http.HttpRequest
import net.jkcode.jkmvc.http.HttpResponse
import net.jkcode.jkmvc.http.setAttributes
import net.jkcode.jkutil.collection.LazyAllocatedMap
import net.jkcode.jkutil.common.Config
import java.io.FileNotFoundException
import java.util.*

/**
 * 视图
 *   放在web根目录下的jsp文件
 *
 * @author shijianhang
 * @date 2016-10-21 下午3:14:54
 */
open class View(override val req: HttpRequest, // 请求对象
				override val res: HttpResponse, // 响应对象
				override val file:String, // 视图文件
				tmpVm:Map<String, Any?> // 视图模型
): IView {

	companion object{

		/**
		 * http配置
		 */
		public val config = Config.instance("http", "yaml")

		/**
		 * 视图目录, 根目录为webapp
		 */
		public val viewDir: String = config["viewDir"] ?: "/"
	}

	/**
	 * 视图文件路径
	 */
	override val path:String = viewDir + file

	/**
	 * 视图模型
	 */
	override var vm:MutableMap<String, Any?> =
			if(tmpVm is MutableMap<*, *>)
				tmpVm as MutableMap<String, Any?>
			else if(tmpVm.isEmpty())
                LazyAllocatedMap()
			else
				HashMap(tmpVm)

	/**
	 * 设置视图模型
	 * @param key
	 * @param value
	 * @return
	 */
	public override operator fun set(key:String, value:Any?): View {
		this.vm[key] = value;
		return this;
	}

	/**
	 * 合并视图模型
	 * @param vm
	 * @return
	 */
	public override fun mergeVm(vm: Map<String, Any?>){
		this.vm.putAll(vm)
	}

	/**
	 * 渲染视图
	 */
	public override fun render(){
		// 设置视图模型
		req.setAttributes(vm)

		// 渲染jsp
		val jsp = path + ".jsp"
		val reqDispatcher = req.getRequestDispatcher(jsp) ?: throw FileNotFoundException("RequestDispatcher for resource [$jsp] is null")
		// 在 org.akhikhl.gretty 运行环境中只能使用原始的请求与响应
		// If already included or response already committed, perform include, else forward.
		if(req.isInner || res.isCommitted)
			reqDispatcher.include(req.request, res.response)
		else
			reqDispatcher.forward(req.request, res.response)
	}

	override fun toString(): String {
		return "View(file='$file')"
	}


}
