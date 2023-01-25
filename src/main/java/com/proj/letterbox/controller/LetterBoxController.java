package com.proj.letterbox.controller;

import com.proj.letterbox.model.EmailMessage;
import com.proj.letterbox.model.Letter;
import com.proj.letterbox.model.LetterBox;
import com.proj.letterbox.model.User;
import com.proj.letterbox.repository.LetterBoxRepository;
import com.proj.letterbox.repository.UserRepository;
import com.proj.letterbox.service.EmailService;
import com.proj.letterbox.service.LetterBoxService;
import com.proj.letterbox.service.LetterService;
import com.proj.letterbox.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/letterbox")
public class LetterBoxController {
    @Autowired
    LetterBoxService letterBoxService;
    @Autowired
    LetterService letterService;
    @Autowired
    UserService userService;
    @Autowired
    EmailService emailService;

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    //조회
    @ResponseBody
    @GetMapping(value = "/{letterboxIdx}")
    public ResponseEntity<Object> findLetterBoxByUserIdx(HttpServletRequest request, @PathVariable("letterboxIdx") int letterboxIdx) {
        LetterBox getLetterBox = letterBoxService.getLetterBoxById(letterboxIdx);
        if (request.getHeader("authorization") != null) {
            User user = userService.getUser(request);
            if (user == getLetterBox.getOwner())
                return findMyLetterBox(request);
        }
        LetterBox returnLetterBox = new LetterBox(getLetterBox.getLetterboxId(), getLetterBox.getName(), getLetterBox.getLetterList());
        return ResponseEntity.ok().body(returnLetterBox);
    }
    //내 복주머니 조회
    @ResponseBody
    @GetMapping("/my")
    public ResponseEntity<Object> findMyLetterBox(HttpServletRequest request) {
        User user = userService.getUser(request);
        LetterBox getLetterBox = letterBoxService.findLetterBoxByUserIdx(user.getUserCode());
        return ResponseEntity.ok().body(getLetterBox);
    }

    @ResponseBody
    @PostMapping("my/letter")
    public ResponseEntity<Object> findMyLetter(HttpServletRequest request) {
        User user = userService.getUser(request);
        List<Letter> letterList = letterService.findMyLetter(user);
        if (letterList.isEmpty())
            return ResponseEntity.badRequest().body("INVALID USER");
        return ResponseEntity.ok().body(letterList);
    }


    //생성
    @ResponseBody
    @PostMapping(value = "")
    public ResponseEntity<Object> saveLetterBox(HttpServletRequest request, @RequestBody LetterBox letterBox) {
        User user = userService.getUser(request);
        logger.debug(user.getEmail());

        LetterBox getLetterBox = letterBoxService.findLetterBoxByUserIdx(user.getUserCode());
        if (getLetterBox == null) {
            LetterBox newLetterBox = letterBoxService.saveLetterBox(user.getUserCode(), letterBox);
            return ResponseEntity.ok().body(newLetterBox);
        }
        else
            return ResponseEntity.badRequest().body("이미 내 복주머니가 존재합니다.");
    }

    @ResponseBody
    @PostMapping(value = "/{letterboxIdx}/letter")
    public ResponseEntity<Object> saveLetter(HttpServletRequest request, @PathVariable("letterboxIdx") int letterboxIdx, @RequestBody Letter letter) {
        System.out.println("뉴레터!" + letter);
        logger.debug("뉴레터!" + letter);
        Letter newLetter = letterService.saveLetter(request, letterboxIdx, letter);

        return ResponseEntity.ok().body(newLetter);
    }

    @ResponseBody
    @GetMapping(value = "/{letterboxIdx}/letter/{letterIdx}")
    public ResponseEntity<Object> getLetter(HttpServletRequest request, @PathVariable("letterboxIdx") int letterboxIdx, @PathVariable("letterIdx") int letterIdx) {
        Letter getLetter = letterService.getLetter(request, letterboxIdx, letterIdx);
        Letter returnLetter = new Letter(getLetter.getLetterId(), getLetter.getLetterBox(), getLetter.getNickname(), getLetter.getContent(), getLetter.getLetterlocation(), getLetter.getFile(), getLetter.getAnswerList(), getLetter.getHintNum(), getLetter.isCorrect());
        return ResponseEntity.ok().body(returnLetter);
    }


    @ResponseBody
    @GetMapping(value = "/{letterboxIdx}/letter/{letterIdx}/add")
    public ResponseEntity<Object> addAnswer(HttpServletRequest request, @PathVariable("letterboxIdx") int letterboxIdx, @PathVariable("letterIdx") int letterIdx, @RequestParam String answer) {
        Letter getLetter = letterService.addAnswer(request, letterboxIdx, letterIdx, answer);
        Letter returnLetter = new Letter(getLetter.getLetterId(), getLetter.getLetterBox(), getLetter.getNickname(), getLetter.getContent(), getLetter.getLetterlocation(), getLetter.getFile(), getLetter.getAnswerList(), getLetter.getHintNum(), getLetter.isCorrect());
        return ResponseEntity.ok().body(returnLetter);
    }

    @ResponseBody
    @GetMapping(value = "/{letterboxIdx}/letter/{letterIdx}/compare")
    public ResponseEntity<Object> compareAnswer(HttpServletRequest request, @PathVariable("letterboxIdx") int letterboxIdx, @PathVariable("letterIdx") int letterIdx, @RequestParam String answer) {
        boolean result = letterService.compareAnswer(request, letterboxIdx, letterIdx, answer);
        Letter letter = letterService.getLetter(request, letterboxIdx, letterIdx);
        LetterBox letterBox = letterBoxService.getLetterBoxById(letterboxIdx);
        if (result == true) {
            EmailMessage emailMessage = EmailMessage.builder()
                    .to(letter.getUser().getEmail())
                    .subject("[레터박스] 정답을 맞혔습니다!")
                    .message("<html><head></head><body><h3>축하드립니다!</h3>\n" +
                            "<p>\n" + letterBox.getName() +
                            "님께서 " + letter.getName() + "님을 맞혔습니다!<br />\n" +
                            letter.getName() + "님도 복주머니를 생성하여 많은 복을 받아보세요!\n" +
                            "</p>\n" +
                            "<a href=\"#letterbox링크\">복주머니 생성하러 가기</a></body></html>")
                    .build();
            boolean res = emailService.sendMail(emailMessage);
            if (res == true)
                return ResponseEntity.ok().body("compare result : " + result + ", email result = success");
            else
                return ResponseEntity.ok().body("compare result : " + result + ", email result = fail");
        }
        return ResponseEntity.ok().body(result);
    }

    @ResponseBody
    @GetMapping(value = "/{letterboxIdx}/letter/{letterIdx}/hint/{hintIdx}")
    public ResponseEntity<Object> getHint(HttpServletRequest request, @PathVariable("letterboxIdx") int letterboxIdx, @PathVariable("letterIdx") int letterIdx, @PathVariable("hintIdx") int hintIdx) {
        String hint = null;
        hint = letterService.getHint(request, letterboxIdx, letterIdx, hintIdx);
        if (hint == null)
            return ResponseEntity.badRequest().body("BAD_HINT_REQUEST");
        else
            return ResponseEntity.ok().body(hint);
    }

    @ResponseBody
    @GetMapping(value = "/{letterboxIdx}/letter/{letterIdx}/hints")
    public ResponseEntity<Object> getHints(HttpServletRequest request, @PathVariable("letterboxIdx") int letterboxIdx, @PathVariable("letterIdx") int letterIdx) {
        ArrayList<String> hints = null;
        hints = letterService.getHints(request, letterboxIdx, letterIdx);
        if (hints.isEmpty())
            return ResponseEntity.badRequest().body("BAD_HINT_REQUEST");
        else
            return ResponseEntity.ok().body(hints);
    }



}