package org.yangxin.socket.lib.core.ds;

/**
 * 带优先级的节点，可用于构成链表
 *
 * @author yangxin
 * 2021/9/15 下午8:58
 */
public class BytePriorityNode<Item> {

    /**
     * 当前字节优先级节点的优先级
     */
    public byte priority;

    /**
     * 当前节点的内容
     */
    public Item item;

    /**
     * 下一个字节优先级节点
     */
    public BytePriorityNode<Item> next;

    public BytePriorityNode(Item item) {
        this.item = item;
    }

    /**
     * 按优先级追加到当前帧链表中
     *
     * @param node node
     */
    public void appendWithPriority(BytePriorityNode<Item> node) {
        if (next == null) {
            // 如果下一个节点为null，则将下一个节点置为新追加的节点
            next = node;
        } else {
            BytePriorityNode<Item> after = this.next;
            if (after.priority < node.priority) {
                // 中间位置插入
                this.next = node;
                node.next = after;
            } else {
                after.appendWithPriority(node);
            }
        }
    }
}
