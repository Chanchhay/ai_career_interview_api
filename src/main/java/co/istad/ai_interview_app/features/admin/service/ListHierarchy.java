package co.istad.ai_interview_app.features.admin.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.function.Function;

/** Rules shared by the three lists. A saved entry keeps its level. */
public final class ListHierarchy {
    private ListHierarchy() {}

    public static <T> T resolveParent(UUID itemId, UUID currentParentId, UUID requestedParentId,
                                      Function<UUID, T> findParent, Function<T, T> parentOf) {
        if (itemId != null && (currentParentId == null) != (requestedParentId == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "An existing entry must keep its level. You can move a subcategory to another parent category.");
        }
        if (requestedParentId == null) return null;
        if (requestedParentId.equals(itemId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An entry cannot be its own parent category.");
        }
        T parent = findParent.apply(requestedParentId);
        if (parentOf.apply(parent) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose a parent category, not another subcategory. Only two levels are allowed.");
        }
        return parent;
    }

    public static void requireNoChildren(boolean hasChildren) {
        if (hasChildren) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Move or delete the subcategories in this parent category before deleting it.");
        }
    }
}
