import 'package:flutter/material.dart';

class MinimalButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool isPrimary;

  const MinimalButton({
    super.key,
    required this.label,
    this.onPressed,
    this.icon,
    this.isPrimary = true,
  });

  @override
  Widget build(BuildContext context) {
    return TextButton(
      onPressed: onPressed,
      style: TextButton.styleFrom(
        backgroundColor: isPrimary ? (onPressed == null ? Colors.grey.shade200 : Colors.black) : Colors.transparent,
        foregroundColor: isPrimary ? Colors.white : Colors.black,
        shape: const RoundedRectangleBorder(borderRadius: BorderRadius.zero),
        padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 24),
        side: isPrimary ? null : const BorderSide(color: Colors.black, width: 1.5),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          if (icon != null) ...[Icon(icon, size: 18), const SizedBox(width: 12)],
          Text(label, style: const TextStyle(fontWeight: FontWeight.bold, letterSpacing: 1.1)),
        ],
      ),
    );
  }
}

class SectionHeader extends StatelessWidget {
  final String title;
  const SectionHeader(this.title, {super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 32, bottom: 16),
      child: Text(
        title.toUpperCase(),
        style: const TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w900,
          letterSpacing: 2,
          color: Colors.black45,
        ),
      ),
    );
  }
}

class FeatureTile extends StatelessWidget {
  final String title;
  final String subtitle;
  final IconData icon;
  final VoidCallback? onTap;

  const FeatureTile({
    super.key,
    required this.title,
    required this.subtitle,
    required this.icon,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 20),
        decoration: BoxDecoration(
          border: Border(bottom: BorderSide(color: Colors.grey.shade200)),
        ),
        child: Row(
          children: [
            Icon(icon, size: 24, color: Colors.black87),
            const SizedBox(width: 20),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  const SizedBox(height: 4),
                  Text(subtitle, style: TextStyle(color: Colors.grey.shade600, fontSize: 13)),
                ],
              ),
            ),
            const Icon(Icons.arrow_forward_ios, size: 14, color: Colors.black26),
          ],
        ),
      ),
    );
  }
}

class SimpleDivider extends StatelessWidget {
  const SimpleDivider({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 1,
      color: Colors.grey.shade100,
    );
  }
}

class MinimalSegmentedControl<T> extends StatelessWidget {
  final List<T> values;
  final List<String> labels;
  final T selectedValue;
  final ValueChanged<T> onSelected;

  const MinimalSegmentedControl({
    super.key,
    required this.values,
    required this.labels,
    required this.selectedValue,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 48,
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.zero,
      ),
      child: Row(
        children: List.generate(values.length, (index) {
          final isSelected = values[index] == selectedValue;
          return Expanded(
            child: GestureDetector(
              onTap: () => onSelected(values[index]),
              child: Container(
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: isSelected ? Colors.black : Colors.transparent,
                  border: isSelected ? null : Border(right: BorderSide(color: Colors.grey.shade200)),
                ),
                child: Text(
                  labels[index].toUpperCase(),
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 1.2,
                    color: isSelected ? Colors.white : Colors.black45,
                  ),
                ),
              ),
            ),
          );
        }),
      ),
    );
  }
}
