package jp.co.sss.lms.controller;

import java.text.ParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.service.StudentAttendanceService;
import jp.co.sss.lms.util.Constants;

/**
 * 勤怠管理コントローラ
 */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {

	@Autowired
	private StudentAttendanceService studentAttendanceService;
	@Autowired
	private LoginUserDto loginUserDto;

	/**
	 * 勤怠管理画面 初期表示
	 */
	@RequestMapping(path = "/detail", method = RequestMethod.GET)
	public String index(Model model) {

		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		boolean notEnterCheck = studentAttendanceService
				.notEnterCheck(attendanceManagementDtoList);
		model.addAttribute("notEnterCheck", notEnterCheck);

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『出勤』ボタン押下
	 */
	@RequestMapping(path = "/detail", params = "punchIn", method = RequestMethod.POST)
	public String punchIn(Model model) {

		String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_ATWORK);
		model.addAttribute("error", error);

		if (error == null) {
			String message = studentAttendanceService.setPunchIn();
			model.addAttribute("message", message);
		}
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『退勤』ボタン押下
	 */
	@RequestMapping(path = "/detail", params = "punchOut", method = RequestMethod.POST)
	public String punchOut(Model model) {

		String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_LEAVING);
		model.addAttribute("error", error);
		if (error == null) {
			String message = studentAttendanceService.setPunchOut();
			model.addAttribute("message", message);
		}
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『勤怠情報を直接編集する』リンク押下
	 */
	@RequestMapping(path = "/update")
	public String update(Model model) {

		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		
		AttendanceForm attendanceForm = studentAttendanceService
				.setAttendanceForm(attendanceManagementDtoList);
		model.addAttribute("attendanceForm", attendanceForm);

		return "attendance/update";
	}

	/**
	 * 勤怠情報直接変更画面 『更新』ボタン押下
	 */
	@RequestMapping(path = "/update", params = "complete", method = RequestMethod.POST)
	public String complete(@ModelAttribute("attendanceForm") AttendanceForm attendanceForm, BindingResult result, Model model)
			throws ParseException {
		
		// 1. バリデーション実行
		studentAttendanceService.updateInputCheck(attendanceForm, result);
		
		// 2. エラーが存在する場合
		if (result.hasErrors()) {
			List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
					.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
			model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);
			
			// 再表示用データの保管
			AttendanceForm tempForm = studentAttendanceService.setAttendanceForm(attendanceManagementDtoList);
			attendanceForm.setHour(tempForm.getHour());
			attendanceForm.setMinute(tempForm.getMinute());
			attendanceForm.setBlankTimes(tempForm.getBlankTimes());

			return "attendance/update"; 
		}
		
		// 3. エラーがない場合のみ更新処理を実行
		studentAttendanceService.formatConversion(attendanceForm);
		
		String message = studentAttendanceService.update(attendanceForm);
		model.addAttribute("message", message);

		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		boolean notEnterCheck = studentAttendanceService
				.notEnterCheck(attendanceManagementDtoList);
		model.addAttribute("notEnterCheck", notEnterCheck);

		return "attendance/detail";
	}
}