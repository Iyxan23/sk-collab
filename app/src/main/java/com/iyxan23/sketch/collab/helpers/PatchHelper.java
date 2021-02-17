package com.iyxan23.sketch.collab.helpers;

import com.iyxan23.sketch.collab.models.Commit;
import com.iyxan23.sketch.collab.models.SketchwareProjectChanges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.fraser.neil.plaintext.diff_match_patch;

public class PatchHelper {

    /**
     * This function reverses a patch, that's it
     * Example:
     *
     * Before:
     * <code>
     * /@@ -3,9 +3,23 @@
     *  llo
     * -World
     * +Earth, I'm Iyxan23!
     * </code>
     *
     * After:
     * <code>
     * /@@ -3,9 +3,23 @@
     *  llo
     * +World
     * -Earth, I'm Iyxan23!
     * </code>
     *
     * The + is reversed into -, and vice versa
     *
     * @param patch The patch string that will be reversed
     * @return The reversed version of the patch parameter given
     */
    public static String reverse_patch(String patch) {
        Pattern plus_pattern = Pattern.compile("^\\+", Pattern.MULTILINE);
        Pattern minus_pattern = Pattern.compile("^-", Pattern.MULTILINE);

        StringBuilder patch_sb = new StringBuilder(patch);

        // Flip + to -
        Matcher matcher = plus_pattern.matcher(patch);
        while (matcher.find()) {
            patch_sb.setCharAt(matcher.start(), '-');
        }

        // Flip - to +
        matcher = minus_pattern.matcher(patch);
        while (matcher.find()) {
            patch_sb.setCharAt(matcher.start(), '+');
        }

        return patch_sb.toString();
    }

    /**
     * This function basically jumps the current patch level to another patch level
     *
     * @param current The current sketchware project in hashmap
     * @param commits ArrayList of commits (should be in a ascending sorted order)
     * @param commit_current The current commit index (should be the index of that commit, cannot be >=size)
     * @param commit_destination The destination commit index (should be the index of that commit, cannot be >=size)
     * @return The current string but in the destination commit index
     */
    public static HashMap<String, String> go_to_commit(HashMap<String, String> current, ArrayList<Commit> commits, int commit_current, int commit_destination) {
        if (commit_destination >= commits.size())
            throw new IndexOutOfBoundsException("commit_destination shouldn't be bigger than commits size");

        String[] project_keys = new String[] {"mysc_project", "logic", "view", "library", "resource", "file"};

        diff_match_patch dmp = new diff_match_patch();

        if (commit_destination > commit_current) {
            // The destination is above us, apply commits that are above the current commit
            for (Commit commit: commits.subList(commit_current + 1, commit_destination)) {
                if (commit.patch == null)
                    continue;

                for (String key: project_keys) {
                    if (!commit.patch.containsKey(key)) continue;

                    LinkedList<diff_match_patch.Patch> patches = (LinkedList<diff_match_patch.Patch>) dmp.patch_fromText(commit.patch.get(key));
                    // TODO: CHECK PATCH STATUSES
                    Object[] result = dmp.patch_apply(patches, current.get(key));

                    current.put(key, (String) result[0]);
                }
            }
        } else if (commit_destination < commit_current) {
            // The destination is below us, apply commits downward
            for (Commit commit: commits.subList(commit_destination, commit_current - 1)) {
                if (commit.patch == null)
                    continue;

                for (String key: project_keys) {
                    if (!commit.patch.containsKey(key)) continue;

                    // Reverse the patch coz we're going downward
                    String reversed_patch = reverse_patch(commit.patch.get(key));

                    LinkedList<diff_match_patch.Patch> patches = (LinkedList<diff_match_patch.Patch>) dmp.patch_fromText(reversed_patch);
                    // TODO: CHECK PATCH STATUSES
                    Object[] result = dmp.patch_apply(patches, current.get(key));

                    current.put(key, (String) result[0]);
                }
            }
        } else {
            return current;
        }

        // Return the final product
        return current;
    }

    /**
     * This function basically converts the patch into a readable patch
     * or convert a Map into a more readable string
     *
     * Example input:
     * <code>
     *     {view=(view patch), logic=(logic patch)}
     * </code>
     *
     * Example output:
     * <code>
     *     view:
     *     (view patch)
     *
     *     logic:
     *     (logic patch)
     * </code>
     * @param patch The Map we're going to convert into
     * @return Readable version of the Map
     */
    public static String convert_to_readable_patch(Map<String, String> patch) {
        StringBuilder result = new StringBuilder();

        // Check if this commit doesn't have any commits
        if (patch != null) {
            for (String key : patch.keySet()) {
                result.append(key).append(":\n").append(patch.get(key));
            }

        } else {
            result.append("This commit doesn't have any patch");
        }

        return result.toString();
    }

    public static String convert_to_readable_patch(SketchwareProjectChanges changes) {
        return convert_to_readable_patch(changes.generatePatch());
    }
}
