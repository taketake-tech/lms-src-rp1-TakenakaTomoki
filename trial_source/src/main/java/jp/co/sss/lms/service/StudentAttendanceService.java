package jp.co.sss.lms.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

@Service
public class StudentAttendanceService {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;

	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId, Integer lmsUserId) {
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}
		return attendanceManagementDtoList;
	}

	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate, Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null && !tStudentAttendance.getTrainingStartTime().equals("")) {
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null || tStudentAttendance.getTrainingStartTime().equals("")) {
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		}
		return null;
	}

	public String setPunchIn() {
		Date date = new Date();
		Date trainingDate = attendanceUtil.getTrainingDate();
		TrainingTime trainingStartTime = new TrainingTime();
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime, null);
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate, Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	public String setPunchOut() {
		Date date = new Date();
		Date trainingDate = attendanceUtil.getTrainingDate();
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate, Constants.DB_FLG_FALSE);
		TrainingTime trainingStartTime = new TrainingTime(tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime, trainingEndTime);
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	public AttendanceForm setAttendanceForm(List<AttendanceManagementDto> attendanceManagementDtoList) {
		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());
		attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());

		LinkedHashMap<Integer, String> hourmap = new LinkedHashMap<>();
		hourmap.put(null, "");
		for (int i = 0; i < 24; i++) {
			hourmap.put(i, String.format("%02d", i));
		}
		attendanceForm.setHour(hourmap);

		LinkedHashMap<Integer, String> minutemap = new LinkedHashMap<>();
		minutemap.put(null, "");
		for (int i = 0; i < 60; i++) {
			minutemap.put(i, String.format("%02d", i));
		}
		attendanceForm.setMinute(minutemap);

		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		for (AttendanceManagementDto attendanceManagementDto : attendanceManagementDtoList) {
			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();
			dailyAttendanceForm.setStudentAttendanceId(attendanceManagementDto.getStudentAttendanceId());
			dailyAttendanceForm.setTrainingDate(dateUtil.toString(attendanceManagementDto.getTrainingDate()));
			dailyAttendanceForm.setTrainingStartTime(attendanceManagementDto.getTrainingStartTime());
			dailyAttendanceForm.setTrainingEndTime(attendanceManagementDto.getTrainingEndTime());
			if (attendanceManagementDto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTime(attendanceManagementDto.getBlankTime());
				dailyAttendanceForm.setBlankTimeValue(String.valueOf(attendanceUtil.calcBlankTime(attendanceManagementDto.getBlankTime())));
			}
			dailyAttendanceForm.setStatus(String.valueOf(attendanceManagementDto.getStatus()));
			dailyAttendanceForm.setNote(attendanceManagementDto.getNote());
			dailyAttendanceForm.setSectionName(attendanceManagementDto.getSectionName());
			dailyAttendanceForm.setIsToday(attendanceManagementDto.getIsToday());
			dailyAttendanceForm.setDispTrainingDate(dateUtil.dateToString(attendanceManagementDto.getTrainingDate(), "yyyy年M月d日(E)"));
			dailyAttendanceForm.setStatusDispName(attendanceManagementDto.getStatusDispName());

			String startTimeString = attendanceManagementDto.getTrainingStartTime();
			if (startTimeString != null && startTimeString.length() >= 5) {
				dailyAttendanceForm.setTrainingStartTimeHour(Integer.parseInt(startTimeString.substring(0, 2)));
				dailyAttendanceForm.setTrainingStartTimeMinute(Integer.parseInt(startTimeString.substring(3, 5)));
			}

			String endTimeString = attendanceManagementDto.getTrainingEndTime();
			if (endTimeString != null && endTimeString.length() >= 5) {
				dailyAttendanceForm.setTrainingEndTimeHour(Integer.parseInt(endTimeString.substring(0, 2)));
				dailyAttendanceForm.setTrainingEndTimeMinute(Integer.parseInt(endTimeString.substring(3, 5)));
			}

			attendanceForm.getAttendanceList().add(dailyAttendanceForm);
		}

		return attendanceForm;
	}

	public String update(AttendanceForm attendanceForm) throws ParseException {
		Integer lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId() : attendanceForm.getLmsUserId();
		List<TStudentAttendance> tStudentAttendanceList = tStudentAttendanceMapper.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);
		Date date = new Date();

		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {
			TStudentAttendance tStudentAttendance = new TStudentAttendance();
			BeanUtils.copyProperties(dailyAttendanceForm, tStudentAttendance);
			tStudentAttendance.setTrainingDate(dateUtil.parse(dailyAttendanceForm.getTrainingDate()));

			for (TStudentAttendance entity : tStudentAttendanceList) {
				if (entity.getTrainingDate().equals(tStudentAttendance.getTrainingDate())) {
					tStudentAttendance = entity;
					break;
				}
			}
			tStudentAttendance.setLmsUserId(lmsUserId);
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());

			TrainingTime trainingStartTime = null;
			if (dailyAttendanceForm.getTrainingStartTime() != null && !dailyAttendanceForm.getTrainingStartTime().trim().isEmpty()) {
				trainingStartTime = new TrainingTime(dailyAttendanceForm.getTrainingStartTime());
				tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());
			} else {
				tStudentAttendance.setTrainingStartTime("");
			}

			TrainingTime trainingEndTime = null;
			if (dailyAttendanceForm.getTrainingEndTime() != null && !dailyAttendanceForm.getTrainingEndTime().trim().isEmpty()) {
				trainingEndTime = new TrainingTime(dailyAttendanceForm.getTrainingEndTime());
				tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());
			} else {
				tStudentAttendance.setTrainingEndTime("");
			}

			tStudentAttendance.setBlankTime(dailyAttendanceForm.getBlankTime());

			if (!"欠席".equals(dailyAttendanceForm.getStatusDispName())) {
				if (trainingStartTime != null || trainingEndTime != null) {
					AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime, trainingEndTime);
					tStudentAttendance.setStatus(attendanceStatusEnum.code);
				} else {
					tStudentAttendance.setStatus(null);
				}
			}

			tStudentAttendance.setNote(dailyAttendanceForm.getNote());
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendanceList.add(tStudentAttendance);
		}

		for (TStudentAttendance tStudentAttendance : tStudentAttendanceList) {
			if (tStudentAttendance.getStudentAttendanceId() == null) {
				tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
				tStudentAttendance.setFirstCreateDate(date);
				tStudentAttendanceMapper.insert(tStudentAttendance);
			} else {
				tStudentAttendanceMapper.update(tStudentAttendance);
			}
		}
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	public boolean notEnterCheck(List<AttendanceManagementDto> attendanceManagementDtoList) {
		try {
			Date today = attendanceUtil.getTrainingDate();
			if (today == null) {
				today = new Date();
			}
			int notEnterCount = tStudentAttendanceMapper.notEnterCount(
					loginUserDto.getLmsUserId(),
					Constants.DB_FLG_FALSE,
					today
			);
			return notEnterCount > 0;
		} catch (Exception e) {
			return false;
		}
	}

	public void formatConversion(AttendanceForm attendanceForm) {
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {
			if (dailyAttendanceForm.getTrainingStartTimeHour() != null && dailyAttendanceForm.getTrainingStartTimeMinute() != null) {
				dailyAttendanceForm.setTrainingStartTime(String.format("%02d:%02d", dailyAttendanceForm.getTrainingStartTimeHour(), dailyAttendanceForm.getTrainingStartTimeMinute()));
			} else {
				dailyAttendanceForm.setTrainingStartTime(null);
			}

			if (dailyAttendanceForm.getTrainingEndTimeHour() != null && dailyAttendanceForm.getTrainingEndTimeMinute() != null) {
				dailyAttendanceForm.setTrainingEndTime(String.format("%02d:%02d", dailyAttendanceForm.getTrainingEndTimeHour(), dailyAttendanceForm.getTrainingEndTimeMinute()));
			} else {
				dailyAttendanceForm.setTrainingEndTime(null);
			}
		}
	}

	public void updateInputCheck(AttendanceForm attendanceForm, BindingResult result) {
		List<DailyAttendanceForm> attendanceList = attendanceForm.getAttendanceList();
		if (attendanceList == null) {
			return;
		}

		for (int i = 0; i < attendanceList.size(); i++) {
			DailyAttendanceForm dailyForm = attendanceList.get(i);

			if (dailyForm.getNote() != null && dailyForm.getNote().length() > 100) {
				result.rejectValue("attendanceList[" + i + "].note", "maxlength");
			}

			boolean hasStartHour = dailyForm.getTrainingStartTimeHour() != null;
			boolean hasStartMinute = dailyForm.getTrainingStartTimeMinute() != null;
			boolean hasEndHour = dailyForm.getTrainingEndTimeHour() != null;
			boolean hasEndMinute = dailyForm.getTrainingEndTimeMinute() != null;

			if (hasStartHour != hasStartMinute) {
				result.rejectValue("attendanceList[" + i + "].trainingStartTimeHour", "input.invalid");
			}

			if (hasEndHour != hasEndMinute) {
				result.rejectValue("attendanceList[" + i + "].trainingEndTimeHour", "input.invalid");
			}

			boolean hasStart = hasStartHour && hasStartMinute;
			boolean hasEnd = hasEndHour && hasEndMinute;

			if (!hasStart && hasEnd) {
				result.rejectValue("attendanceList[" + i + "].trainingStartTimeHour", "attendance.punchInEmpty");
			}

			if (hasStart && hasEnd) {
				int startMinutes = dailyForm.getTrainingStartTimeHour() * 60 + dailyForm.getTrainingStartTimeMinute();
				int endMinutes = dailyForm.getTrainingEndTimeHour() * 60 + dailyForm.getTrainingEndTimeMinute();

				if (startMinutes > endMinutes) {
					result.rejectValue("attendanceList[" + i + "].trainingEndTimeHour", "attendance.trainingTimeRange");
				}

				if (dailyForm.getBlankTime() != null && dailyForm.getBlankTime() > 0) {
					int maxMinutes = endMinutes - startMinutes;
					if (dailyForm.getBlankTime() > maxMinutes) {
						result.rejectValue("attendanceList[" + i + "].blankTime", "attendance.blankTimeError");
					}
				}
			}
		}
	}
}