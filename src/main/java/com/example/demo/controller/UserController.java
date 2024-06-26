package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.dto.UserDTO;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Validated
public class UserController {
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private UserService userService;

    //이메일 유효
    private boolean isValidEmail(String email) {
        return email.matches("^[_a-zA-Z0-9-]+(.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+(.[a-zA-Z0-9-]+)*(.[a-zA-Z]{2,})$");
    }



   //회원가입
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> joinUser(
            @RequestParam("file") MultipartFile file,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("passwordConfirm") String passwordConfirm,
            @RequestParam("nickname") String nickname,
            @RequestParam("career") String career,
            @RequestParam("salary") Integer salary,
            @RequestParam("saving") Integer saving,
            @RequestParam("ageRange") Integer ageRange,
            @RequestParam("introduction") String introduction) {

        // 이메일 입력했는지 확인
        if (StringUtils.isEmpty(email)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "이메일을 입력해주세요.");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        // 올바른 형식인지 확인
        if (!isValidEmail(email)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "올바른 이메일 형식이 아닙니다.");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        // 조건에 맞는 비밀번호인지 확인
        if (!isValidPassword(password)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "조건에 맞지 않는 비밀번호입니다. 영어, 숫자, 특수문자를 조합하여 10글자 이상으로 설정해주세요.");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        // 입력한 비밀번호와 맞는지 확인
        if (!password.equals(passwordConfirm)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "일치하지 않는 비밀번호입니다. 다시 입력해주세요.");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        // 이미 있는 이메일인지 확인
        if (userService.existsByEmail(email)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "이미 가입된 이메일 주소입니다.");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        // 사진 저장
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String filePath = uploadDir + "/" + fileName;
        try {
            Files.copy(file.getInputStream(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();

        }


        UserDTO userDTO = new UserDTO();
        userDTO.setEmail(email);
        userDTO.setPassword(password);
        userDTO.setNickname(nickname);
        userDTO.setCareer(career);
        userDTO.setSalary(salary);
        userDTO.setSaving(saving);
        userDTO.setAgeRange(ageRange);
        userDTO.setIntroduction(introduction);
        userDTO.setProfile(filePath);


        User savedUser = userService.saveUser(userDTO);
        if (savedUser != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("userId", savedUser.getId());
            response.put("message", "회원가입이 완료되었습니다. 월 5천원씩 내시면 모든 기능을 자유롭게 이용하실 수 있습니다!");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "회원가입에 실패했습니다.");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean containsLetter(String password) {
        // 영어 포함 여부 확인
        return password.matches(".*[a-zA-Z].*");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody UserDTO userDTO) {
        User user = userService.login(userDTO.getEmail(), userDTO.getPassword());
        if (user != null) {
            // 로그인 성공 메시지와 함께 사용자 ID 반환
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());
            response.put("message", "로그인이 완료되었습니다. 월 만 원씩 내시면 모든 기능을 자유롭게 이용하실 수 있습니다!");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            // 로그인 실패 시 에러 메시지 반환
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "이메일 또는 비밀번호가 올바르지 않습니다.");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping("/users")
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable("userId") Long userId) {
        boolean deleted = userService.deleteUser(userId);
        if (deleted) {
            return new ResponseEntity<>("탈퇴가 완료되었습니다.", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/nickname/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "career", required = false) String career) {

        User user = userService.getUserById(userId);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // 프로필 이미지 업로드 처리
//        if (file != null) {
//            // 프로필 이미지 업로드 처리 코드
//        }

        // 닉네임과 직업 업데이트
        if (nickname != null && !nickname.equals(user.getNickname())) {
            user.setNickname(nickname);
        } else if (nickname != null) {
            // 동일한 닉네임이 이미 존재하는 경우
            Map<String, Object> response = new HashMap<>();
            response.put("message", "동일한 닉네임이 이미 존재합니다. 다른 닉네임을 입력하세요.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if (career != null && !career.equals(user.getCareer())) {
            user.setCareer(career);
        } else if (career != null) {
            // 동일한 직업이 이미 존재하는 경우
            Map<String, Object> response = new HashMap<>();
            response.put("message", "동일한 직업이 이미 존재합니다. 다른 직업을 입력하세요.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        User updatedUser = userService.updateUser(user);
        if (updatedUser != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "계정이 수정되었습니다.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/email/{userId}")
    public ResponseEntity<Map<String, Object>> updateEmail(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, String> requestBody) {

        // 사용자 정보 조회
        User user = userService.getUserById(userId);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // 새 이메일 추출
        String newEmail = requestBody.get("email");

        // 입력한 새 이메일과 기존 이메일이 같은지 확인
        if (newEmail.equals(user.getEmail())) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "새 이메일이 현재 이메일과 동일합니다.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // 새 이메일이 올바른 형식인지 확인
        if (!newEmail.matches("^[_a-zA-Z0-9-]+(.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+(.[a-zA-Z0-9-]+)*(.[a-zA-Z]{2,})$")) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "올바른 이메일 형식이 아닙니다.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // 새 이메일이 이미 존재하는지 확인
        if (userService.existsByEmail(newEmail)) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "이미 사용 중인 이메일 주소입니다.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // 이메일 업데이트
        user.setEmail(newEmail);
        User updatedUser = userService.updateUser(user);
        if (updatedUser != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "이메일이 수정되었습니다.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/password/{userId}")
    public ResponseEntity<Map<String, Object>> updatePassword(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, String> requestBody) {

        // 사용자 정보 조회
        User user = userService.getUserById(userId);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // 현재 비밀번호 확인
        String currentPassword = requestBody.get("currentPassword");
        if (!currentPassword.equals(user.getPassword())) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "현재 비밀번호가 올바르지 않습니다. 다시 입력해주세요.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // 새 비밀번호 확인 및 유효성 검사
        String newPassword = requestBody.get("newPassword");
        if (newPassword.equals(currentPassword)) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "변경 비밀번호가 현재 비밀번호와 같습니다.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if (!isValidPassword(newPassword)) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "적합하지 않은 비밀번호입니다. 숫자와 특수기호를 포함하여 10글자 이상 입력해주세요.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // 비밀번호 업데이트
        user.setPassword(newPassword);
        User updatedUser = userService.updateUser(user);
        if (updatedUser != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "비밀번호가 변경되었습니다.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 10 && containsLetter(password) && containsNumber(password) && containsSpecialCharacter(password);
    }

    private boolean containsNumber(String password) {
        // 숫자 포함 여부 확인
        return password.matches(".*\\d.*");
    }

    private boolean containsSpecialCharacter(String password) {
        // 특수기호 포함 여부 확인
        return password.matches(".*[!@#$%^&*()].*");
    }
}