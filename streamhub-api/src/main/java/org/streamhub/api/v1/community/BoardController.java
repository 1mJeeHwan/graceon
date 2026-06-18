package org.streamhub.api.v1.community;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.community.dto.BoardDto;

/**
 * Community board management endpoints (SYSTEM or CHURCH_MANAGER).
 */
@Tag(name = "Board", description = "커뮤니티 게시판 관리")
@RestController
@RequestMapping("/v1/board")
@PreAuthorize("hasAnyAuthority(T(org.streamhub.api.base.security.AuthoritiesConstants).SYSTEM, "
        + "T(org.streamhub.api.base.security.AuthoritiesConstants).CHURCH_MANAGER)")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @Operation(summary = "게시판 목록", description = "관리자용 전체 게시판 목록(정렬순).")
    @PostMapping("/list")
    public ResultDTO<List<BoardDto>> list() {
        return ResultDTO.ok(boardService.listAll());
    }

    @Operation(summary = "게시판 등록")
    @PostMapping
    public ResultDTO<BoardDto> create(@Valid @RequestBody BoardDto request) {
        return ResultDTO.ok(boardService.create(request));
    }

    @Operation(summary = "게시판 수정")
    @PutMapping("/{id}")
    public ResultDTO<BoardDto> update(@PathVariable Long id, @Valid @RequestBody BoardDto request) {
        return ResultDTO.ok(boardService.update(id, request));
    }

    @Operation(summary = "게시판 삭제")
    @DeleteMapping("/{id}")
    public ResultDTO<Void> delete(@PathVariable Long id) {
        boardService.delete(id);
        return ResultDTO.ok();
    }
}
