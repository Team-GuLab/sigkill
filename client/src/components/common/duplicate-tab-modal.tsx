import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/ui/dialog";
import { Button } from "@/ui/button";

interface DuplicateTabModalProps {
  onClose: () => void;
}

/**
 * 다중 탭 간 웹소켓 세션 중복 감지 시 표시되는 모달
 */
export default function DuplicateTabModal({ onClose }: DuplicateTabModalProps) {
  return (
    <Dialog open>
      <DialogContent showCloseButton={false}>
        <DialogHeader>
          <DialogTitle>이미 다른 탭에서 실행 중</DialogTitle>
          <DialogDescription>
            활성화된 기존 탭이 있습니다. 해당 탭으로 돌아가세요.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button onClick={onClose}>방 목록으로 이동</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
