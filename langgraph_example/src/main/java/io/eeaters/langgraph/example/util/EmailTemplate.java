package io.eeaters.langgraph.example.util;

import io.eeaters.langgraph.example.model.Task;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class EmailTemplate {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");

	public String generateAutoReply(Task task, String recipientEmail) {
		return String.format("""
				尊敬的合作伙伴，

				您好！

				我们有一个新的任务安排需要协调：

				任务详情：
				- 任务名称：%s
				- 计划时间：%s
				- 地点：%s

				请您确认以上时间安排是否合适。如果时间有冲突，请告知您方便的时间。

				期待您的回复。

				谢谢！

				自动邮件系统
				""", task.getTitle(), task.getScheduledTime().format(DATE_FORMATTER), task.getLocation());
	}

	public String generateConfirmation(Task task, String recipientEmail) {
		return String.format("""
				尊敬的合作伙伴，

				您好！

				感谢您的确认。我们已收到以下安排：

				确认详情：
				- 任务名称：%s
				- 确认时间：%s
				- 地点：%s

				现在需要与另一方确认相同安排，我们将尽快与您协调最终时间。

				如有任何变更，我们会及时通知您。

				谢谢！

				自动邮件系统
				""", task.getTitle(), task.getFinalConfirmedTime() != null ? task.getFinalConfirmedTime()
				: task.getScheduledTime().format(DATE_FORMATTER), task.getLocation());
	}

	public String generateFinalAgreement(Task task, String recipientEmail) {
		return String.format("""
				尊敬的合作伙伴，

				您好！

				最终安排已确认，详情如下：

				最终确认：
				- 任务名称：%s
				- 最终时间：%s
				- 地点：%s

				所有相关方已确认此安排，请按计划执行。

				如有紧急情况，请及时联系。

				谢谢！

				自动邮件系统
				""", task.getTitle(), task.getFinalConfirmedTime() != null ? task.getFinalConfirmedTime()
				: task.getScheduledTime().format(DATE_FORMATTER), task.getLocation());
	}

	public String generateTimeChangeNotice(Task task, String recipientEmail, String newTime) {
		return String.format("""
				尊敬的合作伙伴，

				您好！

				根据收到的回复，我们需要调整时间安排：

				更新安排：
				- 任务名称：%s
				- 原计划时间：%s
				- 新建议时间：%s
				- 地点：%s

				请您确认新的时间安排是否合适。

				期待您的回复。

				谢谢！

				自动邮件系统
				""", task.getTitle(), task.getScheduledTime().format(DATE_FORMATTER), newTime, task.getLocation());
	}

}