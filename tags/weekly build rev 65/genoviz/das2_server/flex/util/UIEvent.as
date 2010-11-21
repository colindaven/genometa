package util
{
	import flash.events.Event;
	
	public class UIEvent extends flash.events.Event 
	{
		public static var TREE_NODES_EXPANDED:String          = "treeNodesExpanded";
		
		public function UIEvent(eventType:String)
		{
			super(eventType);
		}

	}
}